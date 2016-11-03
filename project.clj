(defproject dunbar-api "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [cheshire "5.5.0"]
                 [traversy "0.4.0"]
                 [bidi "2.0.9"]
                 [scenic "0.2.5"]
                 [http-kit "2.1.18"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler dunbar-api.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]
                        [midje "1.8.3"]]
         :plugins      [[lein-midje "3.2"]]}}
  :main dunbar-api.handler)
