(defproject yxt "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.3.4"]
                 [clj-http "1.1.1"]
                 [org.clojure/data.json "0.2.6"]
                 [ring/ring-defaults "0.1.2"
                  :exclusions [ring/ring-core]]
                 [ring/ring-core "1.4.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [log4j/log4j "1.2.17"
                  :exclusions [javax.mail/mail
                               javax.jms/jms
                               com.sun.jdmk/jmxtools
                               com.sun.jmx/jmxri]]
                 [org.clojure/java.jdbc "0.3.6"]
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 [com.mchange/c3p0 "0.9.5"]
                 [org.clojure/tools.logging "0.3.1"]
                 [clj-time "0.9.0"]
                 [lib-noir "0.9.9"]
                 [com.taoensso/carmine "2.10.0"]
                 [org.clojure/tools.nrepl "0.2.10"]
                 [com.draines/postal "1.11.3"]
                 [org.clojure/clojurescript "0.0-3308"]
                 [org.omcljs/om "0.8.8"]
                 [prismatic/om-tools "0.3.11"]
                 [secretary "1.2.3"]
                 [cljs-ajax "0.3.13"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [prismatic/dommy "1.1.0"]
                 [crypto-password "0.1.3"]
                 [org.eclipse.jetty/jetty-server "9.3.0.v20150612"]
                 [org.eclipse.jetty.websocket/websocket-server "9.3.0.v20150612"]
                 [org.eclipse.jetty.websocket/websocket-servlet "9.3.0.v20150612"]
                 [org.eclipse.jetty.http2/http2-server "9.3.0.v20150612"]
                 [org.clojure/tools.nrepl "0.2.10"]]

  :source-paths ["src/clj" "src/cljs"]
  :plugins [[lein-ring "0.9.6"]
            [lein-cljsbuild "1.0.6"]]
  :main yxt.handler
  :cljsbuild {:builds
              {:dev
               {;; clojurescript source code path
                :source-paths ["src/cljs"]
                ;; Google Closure Compiler options
                :compiler {;; the name of emitted JS script file
                           :output-to "resources/public/js/yxt_dbg.js"
                           ;; minimum optimization
                           :optimizations :whitespace
                           ;; prettyfying emitted JS
                           :pretty-print true}}
               :prod
               {;; clojurescript source code path
                :source-paths ["src/cljs"]
                ;; Google Closure Compiler options
                :compiler {;; the name of emitted JS script file
                           :output-to "resources/public/js/yxt.js"
                           ;; advanced optimization
                           :optimizations :advanced
                           ;; no need prettyfication
                           :pretty-print false}}
               :pre-prod
               {;; clojurescript source code path
                :source-paths ["src/cljs"]
                :compiler {;; different output name
                           :output-to "resources/public/js/yxt_pre.js"
                           ;; simple optimization
                           :optimizations :simple
                           ;; no need prettyfication
                           :pretty-print false}}}}
  :ring {:handler yxt.handler/app
         :nrepl {:start? true
                 :port 9998}}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
