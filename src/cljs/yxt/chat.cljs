(ns yxt.chat
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om]
            [om.dom :as dom]
            [om-tools.dom :as odom]
            [cljs.core.async :refer [<! chan put! timeout]]

            [yxt.ac :as ac]
            [yxt.ac-view :as ac-view]))

(enable-console-print!)

(def app (js/document.getElementById "yxt-main"))

(def chat-stat (atom {:history []
                      :current-user {:tqf {"汤奇峰" 1} :tangqifeng {"汤奇峰" 1}}}))

(defn- render-chathistory
  [data owner]
  (let [time (:time data)
        msg (:message data)
        user (:user data)]
    (odom/div
     {:class (if (= user "Admin")
               "alert alert-info"
               "alert alert-success")}
     (odom/h4
      {:class "alert-heading"}
      (str user "："))
     (odom/p
      {:class "panel-body"}
      msg))))


(defn fchathistory
  [data owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "chathistory")

    om/IRender
    (render [_]
      (render-chathistory data owner))))

(defn- button-send
  [data event {:keys [ws] :as local}]
  (let [input (js/document.querySelector "[name=message]")
        message  (.-value input)]
    (when (not= message "")
      (set! (.-value input) "")
      (.send ws (js/JSON.stringify
                 (clj->js{:message message}))))))

(defn- keydown-send
  [data event {:keys [ws] :as local}]
  (let [input (js/document.querySelector "[name=message]")
        message  (.-value input)]
    (when (and (= 13 (.-keyCode event))
               (not= message ""))
      (set! (.-value input) "")
      (.send ws (js/JSON.stringify
                 (clj->js{:message message}))))))

(defn loading []
  (reify
    om/IRender
    (render [_]
      (dom/span nil " Loading..."))))

(defn suggestions [value data suggestions-ch _]
  (let [users (:current-user data)]
    (go
     (<! (timeout 100))
     (put! suggestions-ch
           (take 10 (set
                     (mapcat
                      (fn [[k v]]
                        (keys v))
                      (reduce
                       (fn [i [k v]]
                         (when (> (.indexOf (name k) value) -1)
                           (assoc i k v)))
                       {} users))))))))

(defn- close
  [_ _ {:keys [ws] :as local}]
  (.close ws))

(defn chat
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:ws (js/WebSocket. (str "ws://" js/location.host "/yxt/ws/") "chat")
       :is-active true
       :result-ch (chan)})

    om/IDidMount
    (did-mount [_]
      (set! (.-onfocus js/window) (fn []
                                    (om/set-state! owner :is-active true)))
      (set! (.-onblur js/window) (fn []
                                    (om/set-state! owner :is-active false)))
      (let [ws (om/get-state owner :ws)]
        (set! (.-onopen ws) (fn [evt]
                              (println "connect success")))
        (set! (.-onmessage ws) (fn [evt]
                                 (let [msg (.-data evt)]
                                   (if (= msg "pong")
                                     (println "heard go on")
                                     (let [d (js->clj
                                              (js/JSON.parse msg)
                                              :keywordize-keys true)]
                                       (when (not (om/get-state owner :is-active))
                                         (if js/Notification
                                           (if (= (.-permission js/Notification) "granted")
                                             (js/Notification.
                                              (:user d)
                                              (clj->js
                                               {:icon (str js/location.origin "/favicon.ico")
                                                :body (:message d)}))
                                             (.requestPermission js/Notification))
                                           (js/alert "Your don't suppoer desktop notification")))
                                       (om/transact!
                                        data :history
                                        #(conj
                                          %
                                          d)))))))
        (set! (.-onerror ws) (fn [evt]
                               (println "error")
                               (om/transact!
                                data
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
                                data
                                :history
                                #(conj % {:user "Admin"
                                          :message "You are leave this room"
                                          :time 1888888}))))
        (let [result-ch (om/get-state owner :result-ch)]
          (go-loop []
            (let [[idx result] (<! result-ch)]
              (js/alert (str "Result is: " result))
              (recur))))
        (js/setInterval
         (fn []
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
          (odom/button {:on-click #(close data % local)
                        :class "btn btn-warning btn-lg btn-block"} "关闭"))
         (odom/div
          nil
          (apply
           odom/div
           nil
           (for [item (:history data)]
             (om/build fchathistory item {:key :time})))
          (odom/div
           nil
           (om/build ac/autocomplete data
                     {:opts
                      {:container-view ac-view/container-view
                       :container-view-opts {}
                       :input-view ac-view/input-view
                       :input-view-opts {:placeholder "Enter anything"}
                       :results-view ac-view/results-view
                       :results-view-opts {:loading-view loading
                                           :render-item ac-view/render-item
                                           :render-item-opts {:text-fn (fn [item _] (str item))}}
                       :result-ch (:result-ch local)
                       :suggestions-fn suggestions}})
           #_(odom/input {:type "text"
                        :name "message"
                        :class "input-xlarge span12"
                        :placeholder "Write your message..."
                        :on-key-down #(keydown-send data % local)}))))
        (odom/div
         nil
         (odom/button {:on-click #(button-send data % local)
                       :class "btn btn-primary btn-lg btn-block"} "发一条试试")))))))



(defn ^:export main
  []
  (om/root chat chat-stat {:target app}))
