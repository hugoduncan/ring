(defproject ring/ring-jetty7-adapter "0.2.5"
  :description "Ring Jetty adapter."
  :url "http://github.com/mmcgrana/ring"
  :dependencies [[org.clojure/clojure "1.2.0-master-SNAPSHOT"]
                 [org.clojure/clojure-contrib "1.2.0-SNAPSHOT"]
                 [ring/ring-core "0.2.5"]
                 [ring/ring-websocket "0.2.5"]
                 [ring/ring-servlet "0.2.5"]
                 [org.eclipse.jetty/jetty-server "7.1.4.v20100610"]
                 [org.eclipse.jetty/jetty-util "7.1.4.v20100610"]
                 [org.eclipse.jetty/jetty-websocket "7.1.4.v20100610"]]
  :dev-dependencies [[lein-clojars "0.5.0"]
                     [swank-clojure "1.2.1"]])
