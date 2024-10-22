(defproject chat-room "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring "1.9.0"]
                 [compojure "1.6.2"]
                 [cheshire "5.10.0"]
                 [com.taoensso/sente "1.16.0"]
                 [org.postgresql/postgresql "42.2.23"]
                 [hiccup "1.0.5"]
                 [com.github.seancorfield/next.jdbc "1.3.883"]
                 [com.taoensso/timbre "6.1.0"]]
  :plugins [[lein-ring "0.12.5"]
            [com.github.liquidz/antq "RELEASE"]]
  :ring {:handler chat-room.core/app}
  :profiles {:dev {:dependencies [[ring/ring-mock "0.4.0"]
                                  [com.bhauman/figwheel-main "0.2.18"]]
                   :resource-paths ["target"]
                   :clean-targets ^{:protect false} ["target"]}}
  :aliases {"fig" ["trampoline" "run" "-m" "figwheel.main"]})
