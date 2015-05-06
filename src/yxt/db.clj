(ns yxt.db
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.data.json :as json]
            [clj-time [coerce :as c] [local :as l]]

            [yxt.key :refer :all]
            [yxt.util :refer :all])
  (:import [com.mchange.v2.c3p0 ComboPooledDataSource]
           [org.postgresql.util PGobject]))


(def db-spec {:classname "org.postgresql.Driver"
              :subprotocol "postgresql"
              :subname (format "//%s:%d/%s" db-host db-port db-name)
              :user db-user
              :passwrod db-pass})

(defn pool
  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))]
    {:datasource cpds}))

(def pooled-db (delay (pool db-spec)))
(defn db-connection [] @pooled-db)

(deflogin tester
  []
  (let [tmp (jdbc/query (db-connection)
                        ["select * from pg_user"])]
    {:body tmp}))

(defmacro insert!
  [table row-map]
  `(jdbc/insert! (db-connection) ~table ~row-map))

(defmacro query
  [sql]
  `(jdbc/query (db-connection) ~sql))

(defmacro update!
  [table new-map where]
  `(jdbc/update! (db-connection) ~table (merge ~new-map
                                               {:update_at (c/to-sql-date l/local-now)}) where))

(defmacro delete!
  [table where]
  `(jdbc/delete! (db-connection) ~table where))

(extend-protocol jdbc/ISQLValue
  clojure.lang.IPersistentMap
  (sql-value [value]
    (doto (PGobject.)
      (.setType "json")
      (.setValue (json/write-str value)))))

(extend-protocol jdbc/IResultSetReadColumn
  PGobject
  (result-set-read-column [pgobj metadata idx]
    (let [type  (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "json" (json/read-str value :key-fn keyword)
        :else value))))

(defn value-to-json-pgobject
  [value]
  (doto (PGobject.)
    (.setType "json")
    (.setValue (json/write-json value))))

(extend-protocol jdbc/ISQLValue
  clojure.lang.IPersistentMap
  (sql-value [value] (value-to-json-pgobject value))

  clojure.lang.IPersistentVector
  (sql-value [value] (value-to-json-pgobject value)))
