(ns yxt.face
  (:require [clj-http.client :as http]
            [noir.session :as ns]
            [clojure.data.json :as json]
            [clojure.java.io :as io]

            [yxt.util :refer :all]
            [yxt.db :refer [insert! query update!]])
  (:use [yxt.key :only [api-key api-secret api-map]])
  (:import [javax.xml.bind DatatypeConverter]))

(defn to-map [^String s]
  (json/read-str s :key-fn keyword))

(defn own []
  (str (java.util.UUID/randomUUID)))

(defn doreq [resp]
  (let [tmp (to-map (:body resp))]
    (println tmp)
    tmp))


(defn create-faceset [^String faceset-name ^String tag]
  (doreq (http/post "https://apicn.faceplusplus.com/v2/faceset/create"
                    {:form-params {:api_key api-key
                                   :api_secret api-secret
                                   :faceset_name faceset-name
                                   :tag tag}})))

(defn get-group-list []
  (doreq (http/get "https://apicn.faceplusplus.com/v2/info/get_group_list"
                   {:query-params {:api_key api-key
                                   :api_secret api-secret}
                    :throw-entire-message? true})))

(defn train-identify
  [group-name]
  (doreq (http/get "https://apicn.faceplusplus.com/v2/train/identify"
                   {:query-params (merge api-map
                                         {:group_name group-name})
                    :throw-entire-message? true})))

(defn face-identify
  ([img group-name]
   (face-identify img group-name "normal"))
  ([img group-name mode]
   (doreq (http/post "https://apicn.faceplusplus.com/v2/recognition/identify"
                     {:multipart [{:name "img" :content img}
                                  {:name "api_key" :content api-key}
                                  {:name "api_secret" :content api-secret}
                                  {:name "group_name" :content group-name}
                                  {:name "mode" :content mode}]
                      :throw-entire-message? true}))))

(defn create-group
  ([group-name]
   (create-group group-name nil))
  ([group-name person-id]
   (doreq (http/post "https://apicn.faceplusplus.com/v2/group/create"
                     {:form-params (merge api-map
                                          (if person-id
                                            {:group_name group-name
                                             :person_id person-id}
                                            {:group_name group-name}))}))))

(defn create-person
  [persone-name face-id gourp-name]
  (doreq (http/post "https://apicn.faceplusplus.com/v2/person/create"
                    {:form-params (merge api-map
                                         {:person_name persone-name
                                          :face_id face-id
                                          :group_name gourp-name})
                     :throw-entire-message? true})))

(defn get-person
  [person-id]
  (doreq (http/get "https://apicn.faceplusplus.com/v2/person/get_info"
                   {:query-params (merge api-map
                                         {:person_id person-id})
                    :throw-entire-message? true})))

(defn detect
  [img]
  (doreq (http/post "http://apicn.faceplusplus.com/v2/detection/detect"
                    {:multipart [{:name "img" :content img}
                                 {:name "api_key" :content api-key}
                                 {:name "api_secret" :content api-secret}
                                 {:name  "attribute" :content "glass,pose,gender,age,race,smiling"}]
                     :throw-entire-message? true})))

(deflogin person-get
  []
  (let [person-id (ns/get :user)
        _ (println ">>>>>>>>>>>>>>>>>>>>>>" person-id)
        body (get-person person-id)]
    (json/write-str body)))


(defn login [id & msg]
  (println ">>>>>>>>>>>>>>>>>>>>>>" msg)
  {:msg "You are login"})

(defn up-pic-face [img pic-name]
  (let [body (detect img)
        face (first (:face body))
        face-id (:face_id face)
        gender (:value (:gender (:attribute face)))
        img-path (.getAbsolutePath img)
        _ (println "<<" pic-name gender)]
    (if (some (fn [x] (= x gender)) (map :group_name (:group (get-group-list)))) ;;按照性别分 Group
      (let [face-handle (face-identify img gender)
            candidate (-> face-handle
                          :face
                          first
                          :candidate)
            high (first candidate)]
        (if (and high (< 80 (:confidence high)))
          (do (login (:person_id high) {:msg high}))
          (let [person-id (:person_id (create-person pic-name face-id gender))]
            (insert! :yxt_user {:pic_path img-path
                                :person_id person-id})
            (train-identify gender)
            (login person-id "创建新用户"))))
      (let [group-name (:group_name (create-group gender))
            person-id (:person_id (create-person pic-name face-id group-name))]
        (insert! :yxt_user {:pic_path img-path
                       :person_id person-id})
        (train-identify group-name)
        (login person-id "创建新分组新用户")))))

(defhandler yxt [file]
  (if file
    (cond
      (map? file) (let [{:keys [size tempfile content-type filename]} file
                        new (own)
                        new-name (str new (case content-type
                                            "image/jpeg" ".jpg"
                                            "image/png" ".png"
                                            "image/bmp" ".bmp"
                                            ""))]
                    (io/copy tempfile (io/file "resources" "public" new-name))
                    (if (.startsWith content-type "text")
                      (slurp (str "resources/public/" new-name))
                      (up-pic-face (io/file (str "resources/public/" new-name)) new)))
      (string? file) (let [bimg (DatatypeConverter/parseBase64Binary file)
                           new (own)
                           new-name (str "resources/public/" new ".png")]
                       (with-open [o (io/output-stream new-name)]
                         (.write o bimg))
                       (up-pic-face (io/file new-name) new))
      :default {:error "caonima"})
    {:error "file is required"}))
