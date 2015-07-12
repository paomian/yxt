(ns yxt.chat
  (:require [om.core :as om]
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
    (odom/li nil
             (odom/span (str user ": " msg)))))

(defn fchathistory
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "chathistory")

    om/IRender
    (render [_]
      (render-chathistory state owner))))

(defn- send
  "On tasklist form submit callback."
  [state event {:keys [ws] :as local}]
  (let [input (js/document.querySelector "[name=message]")
        message  (.-value input)]
    (set! (.-value input) "")
    (.send ws message)))

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
        nil
        (odom/div
         nil
         (apply odom/ul
                (for [item (:history state)]
                  (om/build fchathistory item {:key :time}))))
        (odom/div
         nil
         (odom/input {:type "text"
                      :name "message"
                      :placeholder "Write your message..."})
         (odom/button {:on-click #(send state % local)} "发一条试试")
         (odom/button {:on-click #(close state % local)} "关闭")))))))

(defn ^:export main
  []
  (om/root chat chathistory {:target app}))
