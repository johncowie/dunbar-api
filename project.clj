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
                 [http-kit "2.1.18"]
                 [ragtime "0.6.1"]
                 [yesql "0.5.3"]
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 [com.zaxxer/HikariCP "2.4.7"]
                 [com.stuartsierra/component "0.3.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail javax.jms/jms com.sun.jmdk/jmxtools com.sun.jmx/jmxri]]
                 [org.slf4j/slf4j-log4j12 "1.7.21"]
                 [environ "1.1.0"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler dunbar-api.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]
                        [midje "1.8.3"]]
         :plugins      [[lein-midje "3.2"]
                        [lein-environ "1.1.0"]]
         :env {:postgres-uri "postgresql://localhost:5432/dunbar"}}}
  :main dunbar-api.handler)
