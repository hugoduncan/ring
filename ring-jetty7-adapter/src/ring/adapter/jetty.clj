(ns ring.adapter.jetty
  "Adapter for the Jetty 7 webserver."
  (:import (org.eclipse.jetty.server.handler AbstractHandler)
           (org.eclipse.jetty.server Server Request Response)
           (org.eclipse.jetty.server.bio SocketConnector)
           (org.eclipse.jetty.server.ssl SslSocketConnector)
           (org.eclipse.jetty.websocket WebSocketHandler)
           (javax.servlet.http HttpServletRequest HttpServletResponse))
  (:use (ring.util servlet)
        (clojure.contrib except java-utils)))

(defn- proxy-handler
  "Returns an Jetty Handler implementation for the given Ring handler."
  [handler]
  (proxy [AbstractHandler] []
    (handle [target #^Request base-request request response]
      (let [request-map  (build-request-map request)
            response-map (handler request-map)]
        (when response-map
          (update-servlet-response response response-map)
          (.setHandled request true))))))

(defn websocket-proxy-handler
  "Returns an Jetty Handler implementation for the given Ring handler.  The
   implementation will forward websocket connection requests to the handler
   with a :request-method of :websocket-connect, and with :websocket-protocol
   set to the requested websocket protocol.  To accept a websocket connection
   the handler should set the :websocket entry in the response map to the
   result of a websocket-proxy call."
  [handler]
  (proxy [WebSocketHandler] []
    (handle [target #^Request base-request request response]
      (if (= "WebSocket" (.getHeader request "Upgrade"))
        (let [protocol (.getHeader request "WebSocket-Protocol")
              websocket (.doWebSocketConnect this request protocol)
              host (.getHeader request "Host")
              origin (.getHeader request "Origin")
              origin (.checkOrigin this request host origin)]
          (if websocket
            (.upgrade
             (wall-hack-field WebSocketHandler '_websocket this)
             request response websocket origin protocol)
            (.sendError response 503)))
        (let [request-map  (build-request-map request)
              response-map (handler request-map)]
          (when response-map
            (update-servlet-response response response-map)
            (.setHandled request true)))))
    (doWebSocketConnect
     [#^HttpServletRequest request #^String protocol]
     (let [request-map  (build-request-map request)
           response-map (handler
                         (merge
                          request-map
                          {:request-method :websocket-connect
                           :websocket-protocol protocol}))]
       (when response-map
         (:websocket response-map))))))

(defn- websocket-connection
  "Create proxy functions for the WebSocketConnection object."
  [connection]
  {:send (fn
           ([data]
              (.sendMessage connection (byte 0) data))
           ([data frame]
              (.sendMessage connection (byte frame) data))
           ([data offset length]
              (.sendMessage connection (byte 0) data offset length))
           ([data offset length frame]
              (.sendMessage connection (byte frame) data offset length)))
   :disconnect (fn [] (.disconnect connection))})

(defn websocket-proxy
  "Create a WebSocket proxy using the supplied functions."
  [{:keys [on-connect on-disconnect on-message]}]
  (proxy [org.eclipse.jetty.websocket.WebSocket]
      []
    (onConnect
     [outbound]
     (on-connect (this outbound)))
    (onDisconnect [] (on-disconnect this))
    (onMessage
     ([frame, data, offset, length]  (on-message this frame data offset length))
     ([frame, data] (on-message this frame data)))))

(defn- add-ssl-connector!
  "Add an SslSocketConnector to a Jetty Server instance."
  [#^Server server options]
  (let [ssl-connector (SslSocketConnector.)]
    (doto ssl-connector
      (.setPort        (options :ssl-port 443))
      (.setKeystore    (options :keystore))
      (.setKeyPassword (options :key-password)))
    (when (options :truststore)
      (.setTruststore ssl-connector (options :truststore)))
    (when (options :trust-password)
      (.setTrustPassword ssl-connector (options :trust-password)))
    (.addConnector server ssl-connector)))

(defn- create-server
  "Construct a Jetty Server instance."
  [options]
  (let [connector (doto (SocketConnector.)
                    (.setPort (options :port 80))
                    (.setHost (options :host)))
        server    (doto (Server.)
                    (.addConnector connector)
                    (.setSendDateHeader true))]
    (when (or (options :ssl?) (options :ssl-port))
      (add-ssl-connector! server options))
    server))

(defn #^Server run-jetty
  "Serve the given handler according to the options.
  Options:
    :configurator   - A function called with the Server instance.
    :port
    :host
    :join?          - Block the caller: defaults to true.
    :ssl?           - Use SSL.
    :ssl-port       - SSL port: defaults to 443, implies :ssl?
    :keystore
    :key-password
    :truststore
    :trust-password"
  [handler options]
  (let [#^Server s (create-server (dissoc options :configurator :proxy-handler))]
    (when-let [configurator (:configurator options)]
      (configurator s))
    (doto s
      (.setHandler ((:proxy-handler options proxy-handler) handler))
      (.start))
    (when (:join? options true)
      (.join s))
    s))
