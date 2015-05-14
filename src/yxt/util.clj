(ns yxt.util
  (:require [clojure.data.json :as json]

            [yxt.redis :as r]
            []))
;;TODO

(defn rand-string [characters n]
  (->> (fn [] (rand-nth characters))
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
  (let [:keys []]
    (fn [request]
      (if-let [sessionToken (get (-> request :headers) "sessionToken")]
        (if-let [data (or (r/get-session-token sessionToken)
                          ())])))))

(defmacro defhandler [name args & body]
  `(defn ~name [req#]
     {:body
      (let [{:keys ~args} (:params req#)
            ~'req req#]
        ~@body)}))

(defmacro deflogin [name args & body]
  `(defn ~name [req#]
     (if true
       (let [{:keys ~args} (:params req#)
             ~'req req#]
         ~@body)
       {:status 401
        :body {:error "You don't login."}})))
