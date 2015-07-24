(ns yxt.ws
  (:require [yxt.adapter :refer [send! close!
                                 remote-addr idle-timeout!
                                 idle-timeout connected?
                                 get-req get-resp]]
            [yxt.util :refer [defhandler deflogin] :as yu]
            [yxt.redis :refer [set-cache get-cache]]
            [yxt.db :refer [update!]]

            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [clojure.string :as s])
  (:import [yxt.adapter WebSocketProtocol]))

(def whole (atom {}))

(add-watch
 whole
 :whole (fn [_ _ _ n]
          (log/info n )))

(defn user-msg
  [user msg]
  (json/write-str
   {:message msg
    :user user
    :time (System/currentTimeMillis)}))

(defn admin-msg
  [msg]
  (user-msg "Admin" msg))

(defn notic
  ([oname msg]
   (notic oname msg false))
  ([oname msg admin?]
   (when-not (empty? @whole)
     (dorun (map (fn [[id {:keys [nickname ws]}]]
                   (when ws
                     (try (send! ws
                                 (json/write-str
                                  (if admin?
                                    (admin-msg msg)
                                    (user-msg oname msg))))
                          (catch NullPointerException _
                            (log/errorf "id:%s is close but not clean" id)))))
                 @whole)))))

(defn on-conn
  [data ^WebSocketProtocol ws]
  (let [req (get-req ws)
        cookies (get-in req [:headers :cookie])
        {:keys [_ user]} data
        {:keys [id nickname]} user]
    (let [nickname (or nickname "Lazy")]
      (swap! whole assoc id {:ws ws :nickname nickname})
      (notic nickname (str nickname " join the room") true)
      (send! ws (json/write-str (admin-msg
                                 (str
                                  "Current room users : "
                                  (clojure.string/join
                                   ", "
                                   (map (fn [[_ m]] (:nickname m)) @whole)))))))))

(defn on-close
  [data ^WebSocketProtocol ws status reason]
  (let [{:keys [key user]} data]
    (log/infof "%s close ws,code:%s reason:%s" user status reason)
    (when-not (empty? @whole)
      (let [nickname (:nickname
                      (get @whole (:id user)))]
        (swap! whole dissoc (:id user))
        (notic ws (str nickname " leavl the room") true)))))

;(update! :yxt_user {:person_id person-id} ["session_token = ?" session-token])

(defn change-name
  [user ^String message
   ^WebSocketProtocol ws]
  (let [n (.substring message 6)
        tmp (get-cache "yxt:name:" (:id user))
        {:keys [id]} user]
    (if tmp
      "You change name too fast"
      (let [old (get-cache key)]
        (log/infof "%s change name to %s" id n)
        (update! :yxt_user {:nickname n} ["id = ?" id])
        (set-cache "yxt:name:" id "1" "EX" 84600)
        (set-cache key (assoc-in old [:yxt :user :nickname] n))
        (swap! whole assoc id {:ws ws :nickname n})
        (format "Change name to %s success" n)))))

(defn on-text
  [data ^WebSocketProtocol ws ^String text-message]
  (when (not= text-message "")
    (let [{:keys [key user]} data]
      (cond
        (= text-message "ping") (send! ws "pong")
        :default
        (let [{:keys [message]}
              (try (json/read-str text-message
                                  :key-fn keyword)
                   (catch Exception _
                     (log/warnf "%s send message: %s"
                                (get @whole ws) text-message)))]
          (cond
            (.startsWith message "/name")
            (send! ws (admin-msg (change-name user message ws)))

            (identity message)
            (let [nickname (get-in @whole [(:id user) :nickname] "傻逼!")]
              (log/infof "%s send message: %s" nickname text-message)
              (notic nickname message))
            :default
            (send! ws (admin-msg "You send a invalid message."))))))))

(def ws-handler
  {:on-connect on-conn
   :on-error (fn [data ^WebSocketProtocol ws e]
               (log/error e))
   :on-close on-close
   :on-text on-text
   :on-bytes (fn [data ^WebSocketProtocol ws b offset len]
               (send! ws (admin-msg "You send a invalid message.")))})
