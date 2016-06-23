(defproject link/link-epoll "0.2.3-SNAPSHOT"
  :description "epoll backend for link tcp module"
  :url "http://github.com/sunng87/link-epoll"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [link "0.8.20"]
                 [io.netty/netty-transport-native-epoll "4.0.37.Final"
                  :classifier "linux-x86_64"]]
  :profiles {:examples {:source-paths ["examples"]}
             :dev {:dependencies [[log4j/log4j "1.2.17"]]}}
  :deploy-repositories {"releases" :clojars})
