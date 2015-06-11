(ns yxt.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clojure.pprint :as pp]
            [noir.session :as ns]
            [clojure.tools.logging :as log]
            [ring.util.response :as resp]

            [yxt.util :as yu]
            [yxt.wrap :refer :all]
            [yxt.face :as yf]
            [yxt.db :as yd]))

(defmacro mylog
  [s]
  `(log/info (str "\n" (with-out-str (clojure.pprint/pprint ~s)))))


(defn wrap-res
  [handler]
  (fn [req]
    (let [res (handler req)]
      (clojure.pprint/pprint res)
      res)))

(defn wrap-req
  [handler]
  (fn [req]
    (clojure.pprint/pprint req)
    (handler req)))

(def app
  (-> (routes
       (GET "/token" [] *anti-forgery-token*)
       (GET "/" [] (resp/redirect "/video.html"))
       (POST "/yxt" [] yf/yxt)
       (GET "/me" [] yf/person-get)
       (POST "/y" [] yu/tester)
       (GET "/oauth" [] yu/redirect-uri)
       (GET "/callback" [] yu/oauth)
       (GET "/hello" [] "if you get here,you may be lost.")
       (route/resources "/")
       (route/not-found "Not Found"))
      wrap-json
      (wrap-json-body :key-fn keyword)
      wrap-session-token
      (wrap-defaults site-defaults)))
