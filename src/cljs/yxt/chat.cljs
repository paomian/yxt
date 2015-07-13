(ns yxt.chat
  (:require [om.core :as om]
            [om.dom :as dom]
            [om-tools.dom :as odom]))

(enable-console-print!)

(def app (js/document.getElementById "main"))

(def chathistory (atom {:history []}))

(add-watch chathistory :chat
           (fn [_ _ _ n]
             (println n)))

(defn- render-chathistory
  [state owner]
  (let [time (:time state)
        msg (:message state)
        user (:user state)]
    (odom/div
     {:class (if (= user "Admin")
               "panel panel-primary"
               "panel panel-info")}
     (odom/div
      {:class "panel-heading"}
      (str user " 说："))
     (odom/div
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
                              (js/alert "connect success")))
        (set! (.-onmessage ws) (fn [evt]
                                 (let [data (.-data evt)
                                       data (js->clj (js/JSON.parse data) :keywordize-keys true)]
                                   (om/transact! state :history #(conj % data)))))
        (set! (.-onerror ws) (fn [evt]
                               (js/alert "error")))
        (set! (.-onclose ws) (fn [evt]
                               (js/alert "close")))))
    om/IRenderState
    (render-state [_ local]
      (odom/section
       nil
       (odom/div
        {:class "container"}
        (apply
         odom/div
         nil
         (odom/button {:on-click #(close state % local)
                       :class "btn btn-warning btn-lg btn-block"} "关闭")
         (for [item (:history state)]
           (om/build fchathistory item {:key :time})))
        (odom/div
         nil
         (odom/div
          {:class "row"}
          (odom/div
           {:class "col-xs-12"}
           (odom/input {:type "text"
                        :name "message"
                        :class "form-control"
                        :placeholder "Write your message..."
                        :on-key-down #(keydown-send state % local)})))
         (odom/button {:on-click #(button-send state % local)
                       :class "btn btn-primary btn-lg btn-block"} "发一条试试")))))))

(defn ^:export main
  []
  (om/root chat chathistory {:target app}))