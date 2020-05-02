(ns link.examples.client
  (:require [link.core :refer :all]
            [link.codec :as codec]
            [link.tcp :as tcp]
            [link.tcp.epoll :as etcp]))

(def line-codec
  (codec/string :delimiter "\r\n" :encoding :utf8))

(def handler
  (create-handler
   (on-message [ch msg]
               (println msg))
   (on-error [ch e]
             (close! ch))))

(defn -main [& args]
  (println "Starting client")
  (let [factory (etcp/tcp-client-factory [(codec/netty-codec line-codec)
                                          handler]
                                         :options {:epoll.tcp_user_timeout (int 10000)
                                                   :epoll.tcp_keepidle (int 2)
                                                   :epoll.tcp_keepintvl (int 3)
                                                   :epoll.tcp_keepcnt (int 5)
                                                  :so_keepalive true})
        client (tcp/tcp-client factory "127.0.0.1" 9930)]
    (send! client "100\r\n")))
