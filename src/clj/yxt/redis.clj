(ns yxt.redis
  (:require [taoensso.carmine :as car :refer (wcar)]
            [taoensso.carmine.ring :refer [carmine-store]]

            [yxt.config :as c]
            [yxt.key :as k]))

(defmacro wcar* [& body] `(car/wcar k/redis-server ~@body))

(defonce session-store
  (carmine-store k/redis-server {:key-prefix "yxt:biepao"
                                 :expiration-secs 2592000}))

(defn set-cache
  ([key data]
   (set-cache nil key data))
  ([prefix key data]
   (wcar* (car/set (str prefix key) data)))
  ([prefix key data opt]
   (wcar* (car/set (str prefix key) data opt)))
  ([prefix key data eox tm]
   (wcar* (car/set (str prefix key) data eox tm))))

(defn del-cache
  ([key]
   (del-cache nil key))
  ([prefix key]
   (wcar* (car/del (str prefix key)))))

(defn get-cache
  ([key]
   (get-cache nil key))
  ([prefix key]
   (wcar* (car/get (str prefix key)))))

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
