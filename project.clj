(defproject link/link-epoll "0.4.0-SNAPSHOT"
  :description "epoll backend for link tcp module"
  :url "http://github.com/sunng87/link-epoll"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [link "0.11.1"]
                 [io.netty/netty-transport-native-epoll "4.1.19.Final"
                  :classifier "linux-x86_64"]]
  :profiles {:examples {:source-paths ["examples"]}
             :dev {:dependencies [[log4j/log4j "1.2.17"]]}}
  :deploy-repositories {"releases" :clojars})
