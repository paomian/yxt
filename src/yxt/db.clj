(ns yxt.db
  (:require [clojure.java.jdbc :as jdbc]
            [yxt.key :refer :all]
            [clojure.data.json :as json])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))


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

(defn tester
  [req]
  (let [tmp (jdbc/query (db-connection)
                        ["select * from pg_user"])]
    {:body tmp}))
