(ns ring.util.servlet.jetty7
  "Compatibility functions for turning a ring handler into a jetty7 Java servlet."
  (:require ring.util.servlet)
  (:import (org.eclipse.jetty.websocket WebSocketServlet)))

(defn make-websocket-connect-method
  "Turns a handler into a function that takes the same arguments and has the
  same return value as the onWebSocketConnect method in the WebSocketServlet class."
  [handler]
  (fn [#^HttpServletRequest request
       #^String protocol]
    (let [request-map (-> request
                          (ring.util.servlet/build-request-map)
                          (merge {:request-method :websocket-connect
                                  :websocket-protocol protocol}))]
      (:websocket (handler request-map)))))

(defn servlet
  "Create a jetty7 servlet from a Ring handler."
  [handler]
  (proxy [WebSocketServlet] []
    (service [request response]
      ((ring.util.servlet/make-service-method handler)
         this request response))
    (doWebSocketConnect [request protocol]
      ((make-websocket-connect-method handler)
       this request protocol))))

(defmacro defservice
  "Defines a service method with an optional prefix suitable for being used by
  genclass to compile a WebSocketServlet class.
  e.g. (defservice my-handler)
       (defservice \"my-prefix-\" my-handler)"
  ([handler]
   `(defservice "-" ~handler))
  ([prefix handler]
     `(defn ~(symbol (str prefix "service"))
        [servlet# request# response#]
        ((ring.util.servlet/make-service-method ~handler)
         servlet# request# response#))
     `(defn ~(symbol (str prefix "doWebSocketConnect"))
        [servlet# request# protocol#]
        ((make-websocket-connect-method ~handler)
         servlet# request# protocol#))))
