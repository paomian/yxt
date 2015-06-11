(ns yxt.util
  (:require [clojure.data.json :as json]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]

            [yxt.db :as d]
            [yxt.key :as yk]))

(defn rand-string [n]
  (->> (fn [] (rand-nth "abcdefghijklmnopqrstuvwxyz1234567890"))
       repeatedly
       (take n)
       (apply str)))

(defmacro defhandler [name args & body]
  (let [verify (:verify (first body))
        [code verify] (if verify
                        [(rest body) verify]
                        [body (list :defaul nil)])]
    `(defn ~name [req#]
       (let [{:keys ~args} (:params req#)
             ~'req req#]
         (if-let [error# (cond ~@verify)]
           {:body {:error error#}})
         (do
           ~@code)))))

(defmacro deflogin [name args & body]
  (let [verify (:verify (first body))
        [code verify] (if verify
                        [(rest body) verify]
                        [body (list :defaul nil)])]
    `(defn ~name [req#]
       (if (:user req#)
         (let [{:keys ~args} (:params req#)
               ~'req req#]
           (if-let [error# (cond ~@verify)]
             {:body {:error error#}}
             (do
               ~@code)))
         {:status 401
          :body {:error "You don't login."}}))))

(defhandler tester
  []
  {:verify [(when-let [hello (-> req
                                 :body
                                 :hello)]
              (not (string? hello))) "hello world"]}
  (let [tmp (d/query
             ["select * from yxt_user"])]
    {:body {:tmp tmp
            :user (:user req)}}))
(defhandler oauth
  [req]
  (let [code (-> req :params :code)
        res (http/post "https://github.com/login/oauth/access_token"
                       {:headers {"Accept" "application/json"}
                        :form-params {:code code
                                      :client_id yk/github-id
                                      :client_secret yk/github-secret}})
        token (:access_token (json/read-str (:body res) :key-fn keyword))]
    (log/infof "token %s is be grant" token)
    {:body (json/read-str (:body (http/get "https://api.github.com/user"
                                           {:headers {"Authorization" (format "token %s" token)}})) :key-fn keyword)}))

(def redirect-uri (format "<a href=\"https://github.com/login/oauth/authorize?client_id=%s&scope=user\">oauth</a>" yk/github-id))
