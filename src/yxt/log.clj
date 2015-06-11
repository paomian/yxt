(ns yxt.log
  (require [clojure.tools.logging :as log]))

(defn facelog [path session-token & reason]
  (log/infof "user %s is login which pic is %s reason:" session-token path reason))
