(ns yxt.wrap
  (:require [clojure.data.json :as json]

            [yxt.redis :as r]
            [yxt.util :refer [*yxt-session* *yxt-cookies*]]
            [yxt.db :as d]
            [yxt.util :refer :all]))

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

(defn wrap-yxt-sc
  "对 *yxt-cookies* 和 *yxt-session* 进行操作，来修改 :session 和 :cookies字段"
  [handler]
  (fn [request]
    (binding [*yxt-session* (atom (get-in request [:session :yxt] {}))
              *yxt-cookies* (atom {})]
      (when-let [resp (handler request)]
        (-> resp
            (assoc :cookies (merge (:cookies resp) @*yxt-cookies*))
            (assoc-in [:session :yxt] @*yxt-session*))))))

(defn wrap-test
  [handler]
  (fn [req]
    (println req)
    (handler req)))
