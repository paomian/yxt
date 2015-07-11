(ns yxt.ws
  (:require [yxt.adapter :refer [send! close!
                                 remote-addr idle-timeout!
                                 idle-timeout content?
                                 get-req get-resp]])
  (:import [yxt.adapter WebSocketProtocol]))

(def whole (atom []))

(add-watch whole :whole (fn [_ _ _ n]
                          (println n)))

(defn on-conn
  [^WebSocketProtocol ws]
  (let [req (get-req ws)]
    (clojure.pprint/pprint req)
    (swap! whole conj ws)
    (send! ws "pong")))

(defn on-close
  [^WebSocketProtocol ws status reason]
  (print status reason)
  (reset! whole []))

(defn on-text
  [^WebSocketProtocol ws ^String text-message]
  (clojure.pprint/pprint (get-req ws))
  (println text-message))

(def ws-handler {:on-connect on-conn
                 :on-error (fn [^WebSocketProtocol ws e] (println "error"))
                 :on-close on-close
                 :on-text on-text
                 :on-bytes (fn [^WebSocketProtocol ws bytes offset len]
                             (println "hello"))})
