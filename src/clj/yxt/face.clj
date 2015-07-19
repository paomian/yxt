(ns yxt.face
  (:require [clj-http.client :as http]
            [noir.session :as ns]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]

            [yxt.util :refer :all]
            [yxt.db :refer [insert! query update!] :as d]
            [yxt.redis :as r]
            [yxt.log :as yl]
            [yxt.hearken :as yh])
  (:use [yxt.key :only [api-key api-secret api-map]])
  (:import [javax.xml.bind DatatypeConverter]))

(defn to-map [^String s]
  (json/read-str s :key-fn keyword))

(defn own []
  (str (java.util.UUID/randomUUID)))

(defn doreq [resp]
  (let [tmp (to-map (:body resp))]
    (log/info tmp)
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
                                   :api_secret api-secret}})))

(defn train-identify
  [group-name]
  (doreq (http/get "https://apicn.faceplusplus.com/v2/train/identify"
                   {:query-params (merge api-map
                                         {:group_name group-name})})))

(defn face-identify
  ([img group-name]
   (face-identify img group-name "normal"))
  ([img group-name mode]
   (doreq (http/post "https://apicn.faceplusplus.com/v2/recognition/identify"
                     {:multipart [{:name "img" :content img}
                                  {:name "api_key" :content api-key}
                                  {:name "api_secret" :content api-secret}
                                  {:name "group_name" :content group-name}
                                  {:name "mode" :content mode}]}))))

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
                                          :group_name gourp-name})})))

(defn get-person
  [person-id]
  (doreq (http/get "https://apicn.faceplusplus.com/v2/person/get_info"
                   {:query-params (merge api-map
                                         {:person_id person-id})})))

(defn detect
  [img]
  (doreq (http/post "http://apicn.faceplusplus.com/v2/detection/detect"
                    {:multipart [{:name "img" :content img}
                                 {:name "api_key" :content api-key}
                                 {:name "api_secret" :content api-secret}
                                 {:name  "attribute" :content "glass,pose,gender,age,race,smiling"}]})))

(deflogin person-get
  []
  (let [person-id (-> req :user :id)
        body (first (query ["select * from yxt_user where id = ?" person-id]))]
    {:body body}))


(defn login [data & {:keys [age gender] :as msg}]
  (println ">>>>>>>>>>>>>>>>>>>>>>" msg)
  (println data)
  (assoc-session! :user data)
  {:user data}
  (merge
   (when (:hello msg)
     {:hello (:hello msg)})
   {:age age :gender gender}))

(defn create-user [path id st & {:keys [age gender] :or {age -1 gender "UNKNOW"}}]
  (let [tmp (-> (insert! :yxt_user {:pic_name path
                                    :person_id id
                                    :session_token st
                                    :age age
                                    :gender gender})
                first
                (select-keys [:id :age :gender :nickname]))]
    (println tmp)
    tmp))

(defn- placeholder
  []
  (str "PENDING-" (System/currentTimeMillis) (rand-string 4)))

(defn up-pic-face [img pic-name]
  (let [body (detect img)]
    (cond (empty? (:face body)) (let [new-path (str "resources/public/no_face" pic-name)]
                                  (log/infof "no face pic %s" new-path)
                                  (.renameTo img (io/file new-path))
                                  {:error "There is no face in your photo"})
          (not (nil? (next (:face body)))) (let [new-path (str "resources/public/no_face" pic-name)]
                                             (log/infof "many face pic %s" new-path)
                                             (.renameTo img (io/file new-path))
                                             {:error "There is not only one face in your phot"})
          (= 1 (count (:face body))) (let [face (first (:face body))
                                           face-id (:face_id face)
                                           gender (-> face :attribute :gender :value)
                                           _ (if (= gender "Female")
                                               (log/infof "email status %s ,face-id:%s, pic-name:%s"
                                                          (send-email "xpaomian@gmail.com" (format "face-id: %s,pic-name:%s" face-id pic-name))
                                                          face-id pic-name))
                                           age (Long/valueOf (-> face :attribute :age :value))
                                           update-person (fn [person-id session-token]
                                                           (update! :yxt_user {:person_id person-id} ["session_token = ?" session-token]))]
                                       (if (some (fn [x] (= x gender)) (map :group_name (:group (get-group-list)))) ;;按照性别分 Group
                                         (let [face-handle (face-identify img gender)
                                               candidate (-> face-handle
                                                             :face
                                                             first
                                                             :candidate)
                                               high (first candidate)]
                                           (if (and high (< 70 (:confidence high)))
                                             (let [person-name (:person_name high)
                                                   user (get-user person-name)
                                                   hearken (first (yh/get-hearken (:id user)))]
                                               (yl/facelog pic-name person-name  "老用户")
                                               (println "old user login" (select-keys user [:id :age :gender :nickname]))
                                               (login (select-keys user [:id :age :gender :nickname])
                                                      :age age
                                                      :gender gender
                                                      :msg "老用户登录"
                                                      :hello hearken))
                                             (let [session-token (rand-string 64)
                                                   user (create-user pic-name (placeholder) session-token :age age :gender gender)]
                                               (future
                                                 (let [start (System/currentTimeMillis)
                                                       person-id (:person_id (create-person session-token face-id gender))]
                                                   (train-identify gender)
                                                   (update-person person-id session-token)  ;; person-name 用来存储 sessionToken
                                                   (log/infof "Create person %s cast time %s ms" person-id (- (System/currentTimeMillis) start))))
                                               (yl/facelog pic-name session-token "新用户" )
                                               (login user
                                                      :age age
                                                      :gender gender
                                                      :msg "创建新用户"))))
                                         (let [session-token (rand-string 64)
                                               user (create-user pic-name (placeholder) session-token :age age :gender gender)]
                                           (future (let [start (System/currentTimeMillis)
                                                         group-name (:group_name (create-group gender))
                                                         person-id (:person_id (create-person session-token face-id group-name))]
                                                     (train-identify group-name)
                                                     (update-person person-id session-token :age age :gender gender)
                                                     (log/infof "Create %s group and person %s cast time %s ms" group-name person-id (- (System/currentTimeMillis) start))))
                                           (yl/facelog pic-name session-token "新分组新用户")
                                           (login user
                                                  :age age
                                                  :gender gender
                                                  :msg "创建新分组新用户"))))
          :default "if you get this page,you are beautiful.")))

(defhandler yxt [file]
  (if file
    (cond
      (map? file) (let [{:keys [size tempfile content-type filename]} file
                        new (own)
                        new-name (str new (case content-type
                                            "image/jpeg" ".jpg"
                                            "image/png" ".png"
                                            "image/bmp" ".bmp"
                                            ""))
                        full-name (str "resources/public/pic/" new-name)]
                    (io/copy tempfile (io/file "resources" "public" "pic" new-name))
                    (if (.startsWith content-type "text")
                      (slurp full-name)
                      {:body (up-pic-face (io/file full-name) new-name)}))
      (string? file) (let [bimg (DatatypeConverter/parseBase64Binary file)
                           new (own)
                           new-name (str new ".png")
                           full-name (str "resources/public/pic/" new-name)]
                       (with-open [o (io/output-stream full-name)]
                         (.write o bimg))
                       {:body (up-pic-face (io/file full-name) new-name)})
      :default {:body {:error "caonima"}})
    {:body {:error "file is required"}}))
