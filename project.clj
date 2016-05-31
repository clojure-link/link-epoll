(defproject link/link-epoll "0.1.0"
  :description "epoll backend for link tcp module"
  :url "http://github.com/sunng87/link-epoll"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [link "0.8.18"]
                 [io.netty/netty-transport-native-epoll "4.0.36.Final"
                  :classifier "linux-x86_64"]]
  :profiles {:examples {:source-paths ["examples"]}}
  :deploy-repositories {"releases" :clojars})
