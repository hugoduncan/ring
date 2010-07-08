(ns ring.adapter.jetty-test
  (:use ring.adapter.jetty :reload-all)
  (:use clojure.test))

;; Simple test just to check syntax
(defn handler [request]
  {:body "hi" :status 200 :headers {"ContentType" "text/plain"}})

;; keep a reference to the server, so we can fix things when developing
(def the-server (atom nil))

(defn stop-running [f]
  (when @the-server
    (.stop @the-server)
    (reset! the-server nil)))

(use-fixtures :each stop-running)

(defn slurp-stream
  "Slurp given stream"
  ([arg] (slurp-stream arg (.name (java.nio.charset.Charset/defaultCharset))))
  ([arg #^String enc]
     (with-open [r (java.io.BufferedReader.
                        (java.io.InputStreamReader. arg enc))]
       (let [sb (StringBuilder.)]
         (loop [c (.read r)]
           (if (neg? c)
             (str sb)
             (do
               (.append sb (char c))
               (recur (.read r)))))))))

(deftest run-jetty-test
  (let [server (reset! the-server (run-jetty handler {:port 0 :join? false}))
        port (.getLocalPort (first (.getConnectors server)))
        url (java.net.URL. "http" "localhost" port "/")
        response (slurp-stream (.openStream url))]
    (is (= "hi\n" response))
    (.stop server)))


(defn socket-channel
  "Construct a non-blocking socket channel."
  [host port]
  (doto (java.nio.channels.SocketChannel/open)
    (.configureBlocking false)
    (.connect (java.net.InetSocketAddress. host port))))

(def outbound-channel (atom nil))

(def ping-seen (atom false))

(defn handler2 [request]
  (if (= :websocket-connect (:request-method request))
    {:websocket
     (reify ring.websocket.WebSocket
            (on-connect [_ outbound]
                        (reset! outbound-channel outbound))
            (on-disconnect [_] )
            (on-message [_ message-map]
                        (if (= (:body message-map) "ping")
                          (reset! @ping-seen true))
                        ((:send @outbound-channel) "pong")))}
    {:body "hi" :status 200 :headers {"ContentType" "text/plain"}}))

;; Ugly state mutating websocket client code
;; http://www.whatwg.org/specs/web-socket-protocol/
(def utf-8 (java.nio.charset.Charset/forName "UTF-8"))
(def client-handshake
     "GET / HTTP/1.1\r\nHost: localhost:%d\r\nConnection: Upgrade\r\nSec-WebSocket-Key2: 12998 5 Y3 1  .P00\r\nSec-WebSocket-Protocol: sample\r\nUpgrade: WebSocket\r\nSec-WebSocket-Key1: 4 @1  46546xW%%0l 1 5\r\nOrigin: http://localhost\r\n\r\n^n:ds[4U")

(def start-of-frame (byte 0))
(def end-of-frame (byte -1))

(defn do-connection
  "Write ping"
  [channel port]
  (let [msg "ping"
        buffer (java.nio.ByteBuffer/allocate (+ (count msg) 2))]
    (.write
     channel
     (doto buffer
       (.put start-of-frame)
       (.put (.getBytes msg utf-8))
       (.put end-of-frame)
       (.rewind)))))

(deftest run-jetty-websocket-test
  (reset! ping-seen false)
  (let [server (reset! the-server
                       (run-jetty
                        handler2 {:port 0 :join? false
                                  :proxy-handler websocket-proxy-handler}))
        port (.getLocalPort (first (.getConnectors server)))
        channel (socket-channel "127.0.0.1" port)
        selector (java.nio.channels.Selector/open)
        state (atom :connecting)
        server-handshake (atom (StringBuilder.))]
    (.register channel selector (.validOps channel))
    (loop [n 20]
      (.select selector 500)
      (doseq [key (.selectedKeys selector)]
        (when (.isConnectable key)
          (when (.isConnectionPending channel)
            (.finishConnect channel))
          ;; WebSocket client handshake
          (.write
           channel
           (java.nio.ByteBuffer/wrap
            (.getBytes (format client-handshake port) utf-8)))
          (Thread/sleep 100))
        (when (.isReadable key)
          (condp = @state
              :connecting
            (let [buffer (java.nio.ByteBuffer/allocate 1024)
                  n (.read channel buffer)]
              (.append @server-handshake (String. (.array buffer) 0 n utf-8))
              (when (.contains (str @server-handshake) "8jKS'y:G*Co,Wxa-")
                (reset! state :connected)
                (do-connection
                 channel
                 (second
                  (re-matches
                   #"Sec-WebSocket-Location: ws://localhost:([0-9]+)/"
                   (str @server-handshake))))))
            :connected
            (let [buffer (java.nio.ByteBuffer/allocate 1024)
                  n (.read channel buffer)
                  msg  (String. (.array buffer) 0 (dec n) utf-8)]
              (when (= msg (str (char 0) "pong"))
                (reset! state :received))))))
      (if (pos? n)
        (recur (dec n))
        (is (= @state :received))))
    (is @ping-seen)
    (.stop server)))

