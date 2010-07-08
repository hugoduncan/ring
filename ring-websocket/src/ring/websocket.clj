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




;; (defprotocol Channel
;;   [connection]
;;   (on-connect )
;;   (send [_ message])
;;   (listen [_ listener]))

;; (defprotocol Connection
;;   (send ([_ message] [_ message frame]) "Send a message over the connection")
;;   (close [_] "Close the connection"))
