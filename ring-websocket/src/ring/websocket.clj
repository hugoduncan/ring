(ns ring.websocket
  (:refer-clojure :exclude [send]))


(defprotocol Connection
  "Server Implementation Protocol"
  (send [_ message] "Send a message over this connection")
  (close [_ ] "Close the connection"))


(defprotocol WebSocket
  "User implemented protocol"
  (on-connect [_ connection-map] "Called by the implementation")
  (on-message [_ message-map] "Called by the implementation or user")
  (on-disconnect [_]))


(deftype SimpleChannel
  [connections handler]
  WebSocket
  (on-connect
   [_ connection-map]
   (swap! connections conj connection-map))
  (on-message
   [_ message-map]
   (let [response (handler message-map)]
     (if-let [body (:body response)]
       (doseq [connection @connections]
         (send connection body)))))
  (on-disconnect [_] nil))

(defn simple-channel
  [handler]
  (SimpleChannel. (atom []) handler))
