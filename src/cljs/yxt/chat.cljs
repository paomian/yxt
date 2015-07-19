(ns yxt.chat
  (:require [om.core :as om]
            [om.dom :as dom]
            [om-tools.dom :as odom]))

(enable-console-print!)

(def app (js/document.getElementById "yxt-main"))

(def chathistory (atom {:history []}))

#_(add-watch chathistory :chat
           (fn [_ _ _ n]
             (println n)))

(defn- render-chathistory
  [state owner]
  (let [time (:time state)
        msg (:message state)
        user (:user state)]
    (odom/div
     {:class (if (= user "Admin")
               "alert alert-info"
               "alert alert-success")}
     (odom/h4
      {:class "alert-heading"}
      (str user " 说："))
     (odom/p
      {:class "panel-body"}
      msg))))


(defn fchathistory
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "chathistory")

    om/IRender
    (render [_]
      (render-chathistory state owner))))

(defn- button-send
  [state event {:keys [ws] :as local}]
  (let [input (js/document.querySelector "[name=message]")
        message  (.-value input)]
    (when (not= message "")
      (set! (.-value input) "")
      (.send ws (js/JSON.stringify
                 (clj->js{:message message}))))))

(defn- keydown-send
  [state event {:keys [ws] :as local}]
  (let [input (js/document.querySelector "[name=message]")
        message  (.-value input)]
    (when (and (= 13 (.-keyCode event))
               (not= message ""))
      (set! (.-value input) "")
      (.send ws (js/JSON.stringify
                 (clj->js{:message message}))))))

(defn- close
  [_ _ {:keys [ws] :as local}]
  (.close ws))

(defn chat
  [state owner]
  (reify
    om/IInitState
    (init-state [_]
      {:ws (js/WebSocket. (str "ws://" js/location.host "/yxt/ws/") "chat")})
    om/IDidMount
    (did-mount [_]
      (let [ws (om/get-state owner :ws)]
        (set! (.-onopen ws) (fn [evt]
                              (println "connect success")))
        (set! (.-onmessage ws) (fn [evt]
                                 (let [data (.-data evt)]
                                   (if (= data "pong")
                                     (println "heard go on")
                                     (om/transact!
                                      state :history
                                      #(conj
                                        %
                                        (js->clj
                                         (js/JSON.parse data)
                                         :keywordize-keys true)))))))
        (set! (.-onerror ws) (fn [evt]
                               (println "error")
                               (om/transact!
                                state
                                :history
                                #(conj % {:user "Admin"
                                          :message (str
                                                    "You are not login please go to "
                                                    js/location.host
                                                    " login")
                                          :time 1888888}))))
        (set! (.-onclose ws) (fn [evt]
                               (println (str "Websocket close code: "
                                             (.-code evt) " reason: "
                                             (.-reason evt)))
                               (om/transact!
                                state
                                :history
                                #(conj % {:user "Admin"
                                          :message "You are leave this room"
                                          :time 1888888}))))
        (js/setInterval (fn []
                          (when (= (.-readyState ws) 1)
                              (.send ws "ping")))
                        10000)))
    om/IRenderState
    (render-state [_ local]
      (odom/section
       (odom/div
        {:class "page-header"}
        (odom/div
         {:class (case (.-readyState (om/get-state owner :ws))
                   0 "alert-info"
                   1 "alert-success"
                   2 "alert-alert-warning"
                   3 "alert-danger"
                   "alert-danger")
          :role="alert"}
         (odom/h2
          {:style {:text-align "center"}}
          (case (.-readyState (om/get-state owner :ws))
            0 "连接中"
            1 "已连接"
            2 "正在关闭"
            3 "已关闭"
            "未知原因"))))
       (odom/div
        {:class "page-header"}
        (odom/div
         nil
         (odom/p
          nil
          (odom/button {:on-click #(close state % local)
                        :class "btn btn-warning btn-lg btn-block"} "关闭"))
         (odom/div
          nil
          (apply
           odom/div
           nil
           (for [item (:history state)]
             (om/build fchathistory item {:key :time})))
          (odom/div
           nil
           (odom/input {:type "text"
                        :name "message"
                        :class "input-xlarge span12"
                        :placeholder "Write your message..."
                        :on-key-down #(keydown-send state % local)}))))
        (odom/div
         nil
         (odom/button {:on-click #(button-send state % local)
                       :class "btn btn-primary btn-lg btn-block"} "发一条试试")))))))



(defn ^:export main
  []
  (om/root chat chathistory {:target app}))
