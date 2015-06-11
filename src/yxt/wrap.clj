(ns yxt.wrap
  (:require [clojure.data.json :as json]

            [yxt.redis :as r]
            [yxt.db :as d]))

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

(defn wrap-test
  [handler]
  (fn [req]
    (println req)
    (handler req)))
