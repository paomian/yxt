(ns yxt.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
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

(defn wrap-req
  [handler]
  (fn [req]
    (mylog req)
    (handler req)))

(def my {:params    {:urlencoded true
                     :multipart  true
                     :keywordize true}
         :responses {:not-modified-responses true
                     :absolute-redirects     true
                     :content-types          true}})

(def json-routes
  (-> (routes
       (GET "/" [] (resp/redirect "/video.html"))
       (POST "/yxt" [] yf/yxt)
       (GET "/me" [] yf/person-get)
       (POST "/y/:foo" [] yu/tester))
      (wrap-routes yu/wrap-session-token)
      (wrap-routes yu/wrap-json-body :key-fn keyword)
      (wrap-routes wrap-defaults my)
      (wrap-routes yu/wrap-json)))



(def not-json-routes
  (-> (routes
       (route/resources "/"))))

(def app
  (-> (routes json-routes
              not-json-routes
              (route/not-found "Not Found"))))
