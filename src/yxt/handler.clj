(ns yxt.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.anti-forgery :as anti]
            [clojure.pprint :as pp]
            [noir.session :as ns]
            [clojure.data.json :as json]

            [yxt.face :as yf]
            [yxt.db :as yd]))

(defroutes app-routes
  (GET "/" [] (str anti/*anti-forgery-token*))
  (POST "/yxt" [] yf/yxt)
  (GET "/me" [] yf/person-get)
  (GET "/y" [] yd/tester)
  (route/not-found "Not Found"))

(defn wrap-json
  [handler]
  (fn [req]
    (let [resp (handler req)
          _ (println resp)
          body (:body resp)]
      (-> resp
          (assoc :body (json/write-str body))
          (assoc-in [:headers "Content-Type"] "application/json;charset=UTF-8")))))

(def app
  (-> app-routes
      (wrap-defaults api-defaults)
      ns/wrap-noir-session
      wrap-json))
