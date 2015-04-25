(ns yxt.face
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.java.io :as io]))

(def api-key "830320c3f29750f6c5cf0d580415aab5")
(def api-secret "ACXKK266tTxZWdCthg-qXj_DxRtW0HzB")

(defmacro defhandler [name args & body]
  (let [args (conj args 'req)]
    `(defn ~name [req#]
       (let [{:keys ~args} (:params req#)]
         ~@body))))

(defn create-faceset [^String faceset-name ^String tag]
  (let [resp (http/post "https://apicn.faceplusplus.com/v2/faceset/create"
                        {:form-params {:api_key api-key
                                       :api_secret api-secret
                                       :faceset_name faceset-name
                                       :tag tag}})]))

(defn get-group-list []
  (let [resp (http/get "https://apicn.faceplusplus.com/v2/info/get_group_list"
                       {:form-params {:api_key api-key
                                      :api_secret api-secret}})
        body (json/read-str (:body resp) :key-fn keyword)]
    body))

(defn face-identify
  ([img name]
   (face-identify img name "noraml"))
  ([img name mode]
   (let [resp (http/post "http://apicn.faceplusplus.com/v2/detection/detect"
                         {:multipart [{:name "img" :content img}
                                      {:name "api_key" :content api-key}
                                      {:name "api_secret" :content api-secret}
                                      {:name "group_name" :content name}
                                      {:name "mode" :content mode}]
                          :throw-entire-message? true})])))

(defn create-group
  [name])

(defn up-pic-face [img]
  (let [resp (http/post "http://apicn.faceplusplus.com/v2/detection/detect"
                        {:multipart [{:name "img" :content img}
                                     {:name "api_key" :content api-key}
                                     {:name "api_secret" :content api-secret}
                                     {:name  "attribute" :content "glass,pose,gender,age,race,smiling"}]
                         :throw-entire-message? true})
        body (json/read-str (:body resp) :key-fn keyword)
        gender (:value (:gender (:attribute (get (:face body) 0))))]
    (if (some (fn [x] (= x gender)) (map :group_name (:group (get-group-list))))
      )
    body))

(defhandler yxt [file]
  (let [{:keys [size tempfile content-type filename]} file]
    (io/copy tempfile (io/file "resources" "public" filename))
    (if (.startsWith content-type "text")
      (slurp (str "resources/public/" filename))
      ((up-pic-face (io/file (str "resources/public/" filename)))))))
