(defproject yxt "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.3.1"]
                 [clj-http "1.1.1"]
                 [org.clojure/data.json "0.2.6"]
                 [ring/ring-defaults "0.1.2"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [postgresql "9.4-1201.jdbc41"]
                 [com.mchange/c3p0 "0.9.5"]
                 [lib-noir "0.9.9"]]
  :plugins [[lein-ring "0.8.13"]]
  :ring {:handler yxt.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
