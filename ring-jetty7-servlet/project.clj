(defproject ring/ring-jetty7-servlet "0.2.5"
  :description "Ring jeety7 servlet utilities."
  :url "http://github.com/mmcgrana/ring"
  :dependencies [[ring/ring-servlet "0.2.5"]
                 [ring/ring-jetty7-adapter "0.2.5"]
                 [org.eclipse.jetty/jetty-websocket "7.1.4.v20100610"]]
  :dev-dependencies [[lein-clojars "0.5.0"]
                     [swank-clojure "1.2.1"]])
