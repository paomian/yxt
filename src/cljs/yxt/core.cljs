(ns yxt.core
  (:require-macros [secretary.core :refer [defroute]]
                   [cljs.core.async.macros :refer [go]])
  (:require [goog.events :as gevents]
            [secretary.core :as secretary]
            [ajax.core :refer [GET POST]]
            [om.core :as om]
            [om-tools.dom :as odom]
            [cljs.core.async :refer [put! chan <!]])
  (:import goog.History))

(defonce yxt-state (atom {:text ""}))

(def app (js/document.getElementById "main"))

(defn hello [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:secondsElapsed 0
       :text ""
       :empty true
       :auto false
       :save (chan)
       :version {:texts [{:text ""}]}})
    om/IDidMount
    (did-mount [_]
      (js/setInterval (fn []
                        (om/update-state! owner :secondsElapsed inc)
                        (let [save (om/get-state owner :save)
                              sec (om/get-state owner :secondsElapsed)]
                          (put! save sec))) 1000))
    om/IWillMount
    (will-mount [_]
      (om/set-state! owner :secondsElapsed 0)
      (let [save (om/get-state owner :save)]
        (go (loop []
              (let [sec (<! save)]
                (when (and (= 10 sec)
                           (not (om/get-state owner :auto)))
                  (om/set-state! owner :auto true)
                  (when-not (= (-> (om/get-state owner [:version :texts])
                                   last
                                   :text)
                               (om/get-state owner :text))
                    (let [n (om/get-state owner :text)]
                      (om/update-state! owner [:version :texts]
                                        (fn [state]
                                          (conj state {:text n}))))
                    (GET "/token"
                         :handler
                         (fn [rsp]
                           (POST "/hello"
                                 {:params {:hello (om/get-state owner :text)}
                                  :format :url
                                  :headers {"X-Csrf-Token" rsp}
                                  :handler (fn [data] (js/console.log data))
                                  :error-handler (fn [data] (js/console.log data))}))
                         :error-handler
                         (fn [rsp]
                           (js/console.log rsp))))))
              (recur)))))
    om/IRenderState
    (render-state [_ {:keys [text empty secondsElapsed]}]
      (odom/section nil
                    (odom/div
                     {:class "row"}
                     (odom/div
                      {:class "col-md-6 col-md-offset-3"}
                      (odom/span nil secondsElapsed)
                      (odom/span nil (om/get-state owner :text))
                      (odom/h1 {:style {:text-align "center"}} "Hello Yxt")
                      (odom/textarea
                       {:class "form-control"
                        :row "3"
                        ;;:defaultValue text
                        :placeholder "Hello Yxt!"
                        :on-change (fn [e]
                                     (om/set-state! owner :auto false)
                                     (om/set-state! owner :secondsElapsed 0)
                                     (let [val (.. e -target -value)]
                                       (om/set-state! owner :text val)
                                       (if (and val
                                                (not= "" val))
                                         (om/set-state! owner :empty false)
                                         (om/set-state! owner :empty true))))})))
                    (odom/br)
                    (odom/br)
                    (odom/div
                     {:class "row"}
                     (odom/div
                      {:style {:text-align "center"}}
                      (odom/button
                       {:type "button"
                        :class "btn btn-success btn-lg"
                        :disabled empty
                        :on-click #(js/console.log "hello")} "Say Hello")))))))

(defroute home-path "/" []
  (om/root hello {}
           {:target app}))

(defroute some-path "/yes" []
  (om/root (fn [data owner]
             (om/component (odom/span nil (:text data))))
           {:text "Yes, We are."}
           {:target app}))

(defroute "*" []
  (om/root (fn [data owner]
             (om/component (odom/span nil "Not Found")))
           yxt-state
           {:target app}))

(defn ^:export main
  []
  ;; Set secretary config for use the hashbang prefix
  (secretary/set-config! :prefix "#")
  ;; Attach event listener to history instance.
  (let [history (History.)]
    (gevents/listen history "navigate"
                    (fn [event]
                      (secretary/dispatch! (.-token event))))
    (.setEnabled history true)))
