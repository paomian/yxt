(ns yxt.redis
  (:require [taoensso.carmine :as car :refer (wcar)]
            [taoensso.carmine.ring :refer [carmine-store]]

            [yxt.config :as c]
            [yxt.key :as k]))

(defmacro wcar* [& body] `(car/wcar k/redis-server ~@body))

(defonce session-store
  (carmine-store k/redis-server {:key-prefix "yxt:biepao"}))

(defn set-cache
  ([prefix st data]
   (wcar* (car/set (str c/session-token-prefix st) data)))
  ([prefix st data opt]
   (wcar* (car/set (str c/session-token-prefix st) data opt)))
  ([prefix st data eox tm]
   (wcar* (car/set (str c/session-token-prefix st) data eox tm))))

(defn del-cache
  [prefix st]
  (wcar* (car/del (str prefix st))))

(defn get-cache
  [prefix st]
  (wcar* (car/get (str prefix st))))

(defn set-session-token
  "设置sessionToken缓存信息"
  [st data]
  (set-cache c/session-token-prefix st data "EX" 86400))

(defn del-session-token
  "删除sessionToken缓存信息"
  [st]
  (del-cache c/session-token-prefix st))

(defn get-session-token
  "获取sessionToken缓存信息"
  [st]
  (get-cache c/session-token-prefix st))
