(defproject ring/ring-jetty-adapter "0.2.3"
  :description "Ring Jetty adapter."
  :url "http://github.com/mmcgrana/ring"
  :dependencies [[ring/ring-core "0.2.3"]
                 [ring/ring-servlet "0.2.3"]
                 [org.eclipse.jetty/jetty-server "7.1.4.v20100610"]
                 [org.eclipse.jetty/jetty-util "7.1.4.v20100610"]
                 [org.eclipse.jetty/jetty-websocket "7.1.4.v20100610"]]
  :dev-dependencies [[lein-clojars "0.5.0"]
                     [swank-clojure "1.2.1"]])
