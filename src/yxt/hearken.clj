(ns yxt.hearken
  (:require [yxt.util :refer :all]
            [yxt.db :refer [insert!] :as d]))

(deflogin hearken
  [hello req]
  {:verify [(when-let [hello (-> req :params :hello)]
              (< 1000 (count hello))) "你输入的太多了。（长度在 1000 个字以下）"]}
  (let [user (:user req)]
    (let [tmp (insert! :yxt_hello {:hello hello
                                   :user_id (:id user)})]
      {:body hello})))
