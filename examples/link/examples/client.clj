(ns link.examples.client
  (:require [link.core :refer :all]
            [link.codec :as codec]
            [link.tcp :as tcp]
            [link.tcp.epoll :as etcp]))

(def line-codec
  (codec/string :delimiter "\r\n" :encoding :utf8))

(def handler
  (create-handler
   (on-active [ch]
              (println "connected." ch))
   (on-message [ch msg]
               (println msg))
   (on-error [ch e]
             (close! ch))
   (on-inactive [ch]
                (println "connection closed." ch))))

(defn -main [& args]
  (println "Starting client")
  (let [factory (etcp/tcp-client-factory [(codec/netty-codec line-codec)
                                          handler]
                                         :options {:epoll.tcp_user_timeout (int 10000) ;; 10 secs
                                                   :epoll.tcp_keepidle (int 2) ;; 2 secs
                                                   :epoll.tcp_keepintvl (int 3) ;; 3 secs
                                                   :epoll.tcp_keepcnt (int 5) ; 5 times
                                                   :so_keepalive true})
        client (tcp/tcp-client factory "127.0.0.1" 9930)]
    (send! client "100\r\n")))

;;
;; linux is required for this example
;;
;; step 0:
;; start wireshark on loopback, set filter to `tcp.port == 9930`
;;
;; step 1:
;; start server: `lein with-profile examples run -m link.examples.server`
;;
;; step 2:
;; start client: `lein with-profile examples run -m link.examples.client`
;;
;; step 3:
;; command for dropping packets on loopback
;; `sudo tc qdisc add dev lo root netem drop 100%`
;;
;; step 4:
;; see the client connection closed
;;
;; step 5:
;; restore lo
;; `sudo tc qdisc del dev lo root netem drop 100%`
;;
