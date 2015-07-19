(ns yxt.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [clojure.pprint :as pp]
            [noir.session :as ns]
            [clojure.tools.logging :as log]
            [ring.util.response :as resp]
            [clojure.tools.nrepl.server :refer [start-server]]
            [ring.middleware.reload :refer [wrap-reload]]
            [clojure.tools.logging :as log]

            [yxt.util :as yu]
            [yxt.wrap :refer :all]
            [yxt.face :as yf]
            [yxt.db :as yd]
            [yxt.redis :as yr]
            [yxt.hearken :refer :all]
            [yxt.adapter :refer [run-jetty]]
            [yxt.ws :refer [ws-handler]]))

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

(def site-defaults
  "A default configuration for a browser-accessible website, based on current
  best practice."
  {:params    {:urlencoded true
               :multipart  true
               :nested     true
               :keywordize true}
   :cookies   true
   :session   {:flash true
               :store yr/session-store
               :cookie-name "yxt-session"
               :cookie-attrs {:http-only true}}
   :security  {:anti-forgery   true
               :xss-protection {:enable? true, :mode :block}
               :frame-options  :sameorigin
               :content-type-options :nosniff}
   :static    {:resources "public"}
   :responses {:not-modified-responses true
               :absolute-redirects     true
               :content-types          true}})

(def app
  (-> (routes
       (GET "/token" [] *anti-forgery-token*)
       (GET "/" [] (resp/redirect "/video.html"))
       (POST "/yxt" [] yf/yxt)
       (POST "/hearken" [] )
       (GET "/me" [] yf/person-get)
       ;;(POST "/y" [] yu/tester)
       ;;(GET "/oauth" [] yu/redirect-uri)
       ;;(GET "/callback" [] yu/oauth)
       (GET "/hello" [] "你知道递归么？")
       (GET "/hhh" [] "你可能不知道递归是什么？")
       (POST "/hello" [] hearken)
       (GET "/byebye" [] (fn [req]
                           (log/infof "byebye by request %s" (pr-str req))
                           (yu/dissoc-session! :session-token) {:body {}}))
       (route/resources "/")
       (route/not-found "你获得了 yxt 独占成就 404"))
      wrap-json
      (wrap-json-body :key-fn keyword)
      wrap-session-token
      wrap-yxt-sc
      (wrap-defaults site-defaults)))

(defonce dev? true)

(defn -main
  [& args]
  (let [handler (if dev?
                  (wrap-reload app)
                  app)]
    (println "Server start port 3000 and repl 7845")
    (start-server :port 7845)
    (run-jetty handler {:websockets {"/yxt/ws" ws-handler}
                        :port 3000})))
