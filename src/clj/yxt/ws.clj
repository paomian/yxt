(ns yxt.ws
  (:require [yxt.adapter :refer [send! close!
                                 remote-addr idle-timeout!
                                 idle-timeout connected?
                                 get-req get-resp]]
            [yxt.util :refer [defhandler deflogin]]
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
                                    {:user "Admin"
                                     :message msg
                                     :time (System/currentTimeMillis)}
                                    {:user oname
                                     :message msg
                                     :time (System/currentTimeMillis)})))
                          (catch NullPointerException _
                            (log/errorf "id:%s is close but not clean" id)))))
                 @whole)))))

(defn on-conn
  [data ^WebSocketProtocol ws]
  (let [req (get-req ws)
        cookies (get-in req [:headers :cookie])
        {:keys [_ user]} data
        {:keys [id nickname]} user]
    (if (nil? nickname)
      (swap! whole assoc id {:ws ws :nickname "hehe"})
      (swap! whole assoc id {:ws ws :nickname nickname}))
    (notic ws (str (or nickname "hehe") " join the room") true)
    (send! ws (json/write-str {:user "Admin"
                               :message (str
                                         "Current room users : "
                                         (clojure.string/join
                                          ", "
                                          (map (fn [[_ m]] (:nickname m)) @whole)))
                               :time (System/currentTimeMillis)}))))

(defn admin-msg
  [message]
  (json/write-str
   {:message message
    :user "Admin"
    :time (System/currentTimeMillis)}))

(defn on-close
  [data ^WebSocketProtocol ws status reason]
  (let [{:keys [key user]} data]
    (log/infof "%s close ws,code:%s reason:%s" user status reason)
    (when-not (empty? @whole)
      (swap! whole dissoc (:id user))
      (notic ws (str (:nickname user) " leavl the room") true))))

;(update! :yxt_user {:person_id person-id} ["session_token = ?" session-token])

(defn on-text
  [data ^WebSocketProtocol ws ^String text-message]
  (when (not= text-message "")
    (let [{:keys [key user]} data
          {:keys [id nickname]} user]
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
            (let [[_ n] (s/split message #"\s+")
                  tmp (get-cache "yxt:name:" (:id user))]
              (if tmp
                (send! ws (admin-msg "You change name too fast"))
                (let [old (get-cache key)]
                  (log/infof "%s change name to %s" (:id user) n)
                  (update! :yxt_user {:nickname n} ["id = ?" id])
                  (set-cache "yxt:name:" (:id user) "1" "EX" 84600)
                  (set-cache key (assoc-in old [:yxt :user :nickname] n))
                  (swap! whole assoc id {:ws ws :nickname n})
                  (send! ws (admin-msg (format "Change name to %s success" n))))))
            (identity message)
            (do
              (log/infof "%s send message: %s" (:nickname user) text-message)
              (notic (:nickname user) message))
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
