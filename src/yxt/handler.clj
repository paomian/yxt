(ns yxt.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.anti-forgery :as anti]
            [clojure.pprint :as pp]
            [noir.session :as ns]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]

            [yxt.util :as yu]
            [yxt.face :as yf]
            [yxt.db :as yd]))

(defroutes app-routes
  (GET "/" [] (str anti/*anti-forgery-token*))
  (POST "/yxt" [] yf/yxt)
  (GET "/me" [] yf/person-get)
  (POST "/y" [] yd/tester)
  (route/not-found "Not Found"))

(defmacro mylog
  [s]
  `(log/info (str "\n" (with-out-str (clojure.pprint/pprint ~s)))))

(defn wrap-json
  [handler]
  (fn [req]
    (let [resp (handler req)
          body (:body resp)]
      (-> resp
          (assoc :body (json/write-str body))
          (assoc-in [:headers "Content-Type"] "application/json;charset=UTF-8")))))

(defn wrap-req
  [handler]
  (fn [req]
    (mylog req)
    (handler req)))

(def app
  (-> app-routes
      wrap-req
      (yu/wrap-json-body :key-fn keyword)
      (wrap-defaults api-defaults)
      wrap-json))
