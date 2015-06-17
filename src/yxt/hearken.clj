(ns yxt.hearken
  (:require [yxt.util :refer :all]
            [yxt.db :refer [insert!] :as d]))

(deflogin hearken
  [hello req]
  (let [user (:user req)]
    (let [tmp (insert! :yxt_hello {:hello hello
                                   :user_id (:id user)})]
      {:body hello})))
