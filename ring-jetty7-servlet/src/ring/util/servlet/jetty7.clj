(ns ring.util.servlet.jetty7
  "Compatibility functions for turning a ring handler into a jetty7 Java servlet."
  (:require ring.util.servlet)
  (:import (org.eclipse.jetty.websocket WebSocketServlet)
           (javax.servlet.http HttpServletRequest HttpServlet)))

(defn make-websocket-connect-method
  "Turns a handler into a function that takes the same arguments and has the
  same return value as the onWebSocketConnect method in the WebSocketServlet class."
  [handler]
  (fn [#^HttpServlet servlet
       #^HttpServletRequest request
       #^String protocol]
    (let [request-map (-> request
                          (ring.util.servlet/build-request-map)
                          (ring.util.servlet/merge-servlet-keys servlet request nil)
                          (merge {:request-method :websocket-connect
                                  :websocket-protocol protocol}))
          response-map (handler request-map)]
      (:websocket response-map))))

(defn websocket-servlet
  "Create a jetty7 servlet from a Ring handler."
  [handler]
  (proxy [WebSocketServlet] []
    (doWebSocketConnect [request protocol]
      ((make-websocket-connect-method handler)
       this request protocol))))

(defmacro def-websocket-service
  "Defines a service method with an optional prefix suitable for being used by
  genclass to compile a WebSocketServlet class.
  e.g. (defservice my-handler)
       (defservice \"my-prefix-\" my-handler)"
  ([handler]
   `(def-websocket-service "-" ~handler))
  ([prefix handler]
     `(defn ~(symbol (str prefix "doWebSocketConnect"))
        [servlet# request# protocol#]
        ((make-websocket-connect-method ~handler)
         servlet# request# protocol#))))
