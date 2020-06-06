(defproject link/link-epoll "0.4.7-SNAPSHOT"
  :description "epoll backend for link tcp module"
  :url "http://github.com/sunng87/link-epoll"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [link "0.12.5"]
                 [io.netty/netty-transport-native-epoll "4.1.50.Final"
                  :classifier "linux-x86_64"]]
  :profiles {:examples {:source-paths ["examples"]}
             :dev {:dependencies [[log4j/log4j "1.2.17"]]}}
  :deploy-repositories {"releases" :clojars})
