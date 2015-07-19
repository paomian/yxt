(ns yxt.db
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.data.json :as json]
            [clj-time
             [format :as f]
             [coerce :as cc]
             [core :as t]]

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
  (f/unparse formatter (t/minus (cc/from-sql-time (k date))
                                       (t/hours 8))))

(defn tras
  [kmap]
  (fn [data]
    (reduce (fn [d [k f]]
              (if (k d)
                (assoc d k (f k d))
                d))
            data (merge {:created_at parse
                         :updated_at parse}
                        kmap))))

(defmacro insert!
  [table row-map & kfun]
  (let [kmap (apply hash-map kfun)]
    `(map (tras ~kmap) (jdbc/insert! (db-connection) ~table ~row-map))))

(defmacro query
  [sql & kfun]
  ;; kfun 形如 {key parse-fn}
  ;; {:created_at parse}
  ;; 为一个 query 的结果指定的 key 做处理
  (let [kmap (apply hash-map kfun)]
    `(map (tras ~kmap) (jdbc/query (db-connection) ~sql))))

(defmacro update!
  [table new-map where & kfun]
  (let [kmap (apply hash-map kfun)]
    `(jdbc/update! (db-connection) ~table ~new-map ~where)))

(defmacro delete!
  [table where & kfun]
  (let [kmap (apply hash-map kfun)]
    (jdbc/delete! (db-connection) ~table ~where)))

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

(defn query-person-for-cache-by-session-token
  [session-token]
  (first
   (query ["select id,email,session_token,age,gender,nickname from yxt_user where session_token = ?" session-token])))

(defn query-person-for-cache-by-id
  [id]
  (first
   (query ["select id,email,session_token from yxt_user where id = ?" id])))
