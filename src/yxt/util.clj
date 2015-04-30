(ns yxt.util)

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

(defn wrap-json-body
  [handler & json-opt]
  (fn [request]
    (if-let [[valid? json] (apply read-json request json-opt)]
      (if valid?
        (handler (assoc request :body json))
        {:status  400
         :headers {"Content-Type" "text/plain"}
         :body    "Malformed JSON in request body."})
      (handler request))))

(defmacro defhandler [name args & body]
  `(defn ~name [req#]
     (let [{:keys ~args} (:params req#)
           ~'req req#]
       ~@body)))

(defmacro deflogin [name args & body]
  `(defn ~name [req#]
     (if true
       (let [{:keys ~args} (:params req#)
             ~'req req#]
         ~@body)
       {:status 401
        :body {:error "You don't login."}})))
