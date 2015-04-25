(ns yxt.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.anti-forgery :as anti]
            [clojure.pprint :as pp]
            [noir.session :as ns]

            [yxt.face :as yf]))

(defroutes app-routes
  (GET "/" [] (str anti/*anti-forgery-token*))
  (POST "/yxt" [] yf/yxt)
  (GET "/me" [] yf/person-get)
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-defaults site-defaults)
      ns/wrap-noir-session))
