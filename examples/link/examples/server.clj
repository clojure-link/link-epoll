(ns link.examples.server
  (:require [link.core :refer :all]
            [link.codec :as codec]
            [link.tcp.epoll :as tcp]))

(def line-codec
  (codec/string :delimiter "\r\n" :encoding :utf8))

(def handler
  (create-handler
   (on-message [ch msg]
               (println (format "%040x" (BigInteger. 1 (.getBytes msg))))
               (send! ch msg))))

(defn -main [& args]
  (println "Starting echo server")
  (tcp/tcp-server 9930 [(codec/netty-encoder line-codec)
                        (codec/netty-decoder line-codec)
                        handler]
                  :options {:epoll.tcp-fastopen (int 256)})
  (println "Echo server started on 9930"))
