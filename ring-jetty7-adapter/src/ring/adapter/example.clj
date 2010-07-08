(ns ring.adapter.example
  (:require
   ring.adapter.jetty
   ring.websocket))

(defn wrap-quote
  "Quote the message"
  [handler]
  (fn [request]
    (update-in (handler request) [:body] (fn [m] (str \" m \")))))

;; A simple echo channel
(def echo (ring.websocket/simple-channel
           (wrap-quote (fn [request] request))))

(def app
     (fn [request]
       (if (= :websocket-connect (:request-method request))
         {:websocket echo})))


;;; Functions to start and stop jetty
(defonce server (atom nil))

(defn- run
  "Run app in jetty"
  [app options]
  (ring.adapter.jetty/run-jetty app options))

(defn start
  "Start the app, keeping track of the server"
  []
  (do
    (reset!
     server
     (run
      app
      {:port 8081
       :join? false
       :proxy-handler ring.adapter.jetty/websocket-proxy-handler}))))

(defn stop
  "Stop the app"
  []
  (swap! server (fn [server] (.stop server) nil)))
