(ns yxt.util
  (:require [clojure.data.json :as json]
            [clj-http.client :as http]

            [yxt.redis :as r]
            [yxt.db :as d]
            [yxt.key :as yk]))

(defn rand-string [n]
  (->> (fn [] (rand-nth "abcdefghijklmnopqrstuvwxyz1234567890"))
       repeatedly
       (take n)
       (apply str)))

(defn- json-request? [request]
  (if-let [type (:content-type request)]
    (not (empty? (re-find #"^application/(.+\+)?json" type)))))

(defn- read-json [request & json-opt]
  (if (json-request? request)
    (if-let [body (:body request)]
      (let [body-string (slurp body)]
        (try
          [true (apply json/read-str body-string json-opt)]
          (catch Exception ex
            [false nil]))))))

(def not-json
  {:status 400
   :handlers {"Content-Type" "application/json"}
   :body {:error "Malformed JSON in request body or Headers is no json"}})

(defn get-user [session-token]
  (or (r/get-session-token session-token)
      (when-let [db-data (d/query-person-for-cache-by-ssssion-token session-token)]
        (r/set-session-token session-token db-data)
        db-data)))

(defn wrap-json
  [handler]
  (fn [req]
    (let [resp (handler req)
          body (:body resp)]
      (if (map? body)
          (-> resp
           (assoc :body (json/write-str body))
           (assoc-in [:headers "Content-Type"] "application/json;charset=UTF-8"))
          resp))))

(defn wrap-json-body
  [handler & json-opt]
  (fn [request]
    (if-let [[valid? json] (apply read-json request json-opt)]
      (if valid?
        (handler (assoc request :body json))
        not-json)
      (handler request))))

(defn wrap-session-token
  [handler & opts]
  (let [{:keys [hello]} opts]
    (fn [request]
      (if-let [session-token (get (-> request :headers) "x-yxt-session-token")]
        (if-let [data (get-user session-token)]
          (handler (assoc request :user data))
          {:status 401
           :headers {"Content-Type" "application/json;charset=UTF-8"}
           :body "{\"error\":\"Malformed SessionToken\"}"})
        (handler request)))))

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
    (log/infof "token %s is be grant")
    {:body (json/read-str (:body (http/get "https://api.github.com/user"
                                           {:headers {"Authorization" (format "token %s" token)}})) :key-fn keyword)}))

(def redirect-uri (format "<a href=\"https://github.com/login/oauth/authorize?client_id=%s&scope=user\">oauth</a>" yk/github-id))
