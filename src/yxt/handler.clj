(ns yxt.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [clojure.pprint :as pp]
            [noir.session :as ns]
            [clojure.tools.logging :as log]
            [ring.util.response :as resp]

            [yxt.util :as yu]
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

(def my {:params    {:urlencoded true
                     :multipart  true
                     :keywordize true}
         :responses {:not-modified-responses true
                     :absolute-redirects     true
                     :content-types          true}})

(def json-routes
  (-> (routes
       (GET "/token" [] *anti-forgery-token*)
       (GET "/" [] (resp/redirect "/video.html"))
       (POST "/yxt" [] yf/yxt)
       (GET "/me" [] yf/person-get)
       (POST "/y" [] yu/tester)
       (GET "/oauth" [] yu/redirect-uri)
       (GET "/callback" [] yu/oauth)
       (GET "/hello" [] (fn [req] (try
                                    (/ 1 0)
                                    (catch Exception e
                                      (log/error e)
                                      {:body "hello"})))))
      #_(wrap-routes wrap-res)
      (wrap-routes yu/wrap-json)
      (wrap-routes wrap-defaults site-defaults)
      (wrap-routes yu/wrap-json-body :key-fn keyword)
      (wrap-routes yu/wrap-session-token)
      #_(wrap-routes wrap-req)))

(def not-json-routes
  (-> (routes
       (route/resources "/"))))

(def app
  (-> (routes json-routes
              not-json-routes
              (route/not-found "Not Found"))))
