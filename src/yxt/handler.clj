(ns yxt.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.anti-forgery :as anti]
            [clojure.pprint :as pp]

            [yxt.face :as yf]))

(defroutes app-routes
  (GET "/" [] (str anti/*anti-forgery-token*))
  (POST "/yxt" [] yf/yxt)
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
