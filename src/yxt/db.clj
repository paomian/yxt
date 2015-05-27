(ns yxt.db
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.data.json :as json]
            [clj-time [format :as f] [coerce :as cc]]

            [yxt.key :refer :all])
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

(def formatter (f/formatter "yyyy-MM-dd HH:mm:ss"))

(defn parse [k date]
  (f/unparse formatter (cc/from-sql-time (k date))))



(defmacro insert!
  [table row-map]
  `(jdbc/insert! (db-connection) ~table ~row-map))

(defmacro query
  [sql & kfun]
  ;; kfun 形如 {key parse-fn}
  ;; {:created_at parse}
  ;; 为一个 query 的结果指定的 key 做处理
  (let [kmap (merge
              {:created_at parse}
              (apply hash-map kfun))]
    `(map (fn [data#] (reduce (fn [d# [k# f#]]
                                (assoc d# k# (f# k# d#)))
                              data# ~kmap))
          (jdbc/query (db-connection) ~sql))))

(defmacro update!
  [table new-map where]
  `(jdbc/update! (db-connection) ~table ~new-map ~where))

(defmacro delete!
  [table where]
  `(jdbc/delete! (db-connection) ~table where))

(defn value-to-json-pgobject [value]
  (doto (PGobject.)
    (.setType "json")
    (.setValue (json/write-str value))))

(extend-protocol jdbc/ISQLValue
  clojure.lang.IPersistentMap
  (sql-value [value] (value-to-json-pgobject value))

  clojure.lang.IPersistentVector
  (sql-value [value] (value-to-json-pgobject value)))

(extend-protocol jdbc/IResultSetReadColumn
  PGobject
  (result-set-read-column [pgobj metadata idx]
    (let [type  (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "json" (json/read-str value :key-fn keyword)
        :else value))))

(defn query-person-for-cache-by-ssssion-token
  [session-token]
  (first
   (query ["select id,email,session_token from yxt_user where session_token = ?" session-token])))

(defn query-person-for-cache-by-id
  [id]
  (first
   (query ["select id,email,session_token from yxt_user where id = ?" id])))
