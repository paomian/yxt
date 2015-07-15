(ns yxt.chat
  (:require [om.core :as om]
            [om.dom :as dom]
            [om-tools.dom :as odom]))

(enable-console-print!)

(def app (js/document.getElementById "main"))

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
    (set! (.-value input) "")
    (.send ws message)))

(defn- keydown-send
  [state event {:keys [ws] :as local}]
  (let [input (js/document.querySelector "[name=message]")
        message  (.-value input)]
    (when (= 13 (.-keyCode event))
      (set! (.-value input) "")
      (.send ws message))))

(defn- close
  [_ _ {:keys [ws] :as local}]
  (.close ws))

(defn chat
  [state owner]
  (reify
    om/IInitState
    (init-state [_]
      {:ws (js/WebSocket. (str "ws://" js/location.host "/yxt/ws/"))})
    om/IDidMount
    (did-mount [_]
      (let [ws (om/get-state owner :ws)]
        (set! (.-onopen ws) (fn [evt]
                              (println "connect success")))
        (set! (.-onmessage ws) (fn [evt]
                                 (let [data (.-data evt)
                                       data (js->clj (js/JSON.parse data) :keywordize-keys true)]
                                   (om/transact!
                                    state :history #(conj % data)))))
        (set! (.-onerror ws) (fn [evt]
                               (println "error")))
        (set! (.-onclose ws) (fn [evt]
                               (println (str "Websocket close code: "
                                             (.-code evt) " reason: "
                                             (.-reason evt)))
                               (om/transact!
                                state
                                :history
                                #(conj % {:user "Admin"
                                          :message "You are leave this room"
                                          :time 1888888}))))))
    om/IRenderState
    (render-state [_ local]
      (odom/section
       nil
       (odom/div
        {:class "container"}
        (odom/div
         {:class "page-header"}
         (odom/h1
          nil (case (.-readyState (om/get-state owner :ws))
                0 "连接中"
                1 "已连接"
                2 "正在关闭"
                3 "已关闭"
                "未知原因")))
        (apply
         odom/div
          {:class "row"}
          (odom/div
           {:class "span12 well"}
           (odom/div
            {:class "span12"}
            (odom/button {:on-click #(close state % local)
                          :class "btn btn-warning btn-lg btn-block"} "关闭")
            (odom/br)(odom/br)(odom/br)(odom/br)
            (odom/div
             {:class "span10"}
             (for [item (:history state)]
              (om/build fchathistory item {:key :time})))
            (odom/br)(odom/br)(odom/br)(odom/br)(odom/br)
            (odom/div
             {:class "span10"}
             (odom/input {:type "text"
                          :name "message"
                          :class "input-xlarge span10"
                          :placeholder "Write your message..."
                          :on-key-down #(keydown-send state % local)})
             (odom/br)(odom/br)))
           (odom/br)(odom/br)
           (odom/div
            {:class "span12"}
            (odom/button {:on-click #(button-send state % local)
                          :class "btn btn-primary btn-large span10"} "发一条试试"))
           (odom/br)(odom/br)(odom/br)(odom/br))))))))



(defn ^:export main
  []
  (om/root chat chathistory {:target app}))
