(ns link.http.epoll
  (:require [link.tcp.epoll :as tcp]
            [link.http :as http]
            [link.threads :as threads])
  (:import [io.netty.handler.codec.http
            HttpRequestDecoder
            HttpObjectAggregator
            HttpResponseEncoder]))

(defn http-server [port ring-fn
                   & {:keys [threads executor debug host
                             max-request-body
                             options]
                      :or {threads nil
                           executor nil
                           debug false
                           host "0.0.0.0"
                           max-request-body 1048576}}]
  (let [executor (if threads (threads/new-executor threads) executor)
        ring-handler (http/create-http-handler-from-ring ring-fn debug)
        handlers [(fn [_] (HttpRequestDecoder.))
                  (fn [_] (HttpObjectAggregator. max-request-body))
                  (fn [_] (HttpResponseEncoder.))
                  {:executor executor
                   :handler ring-handler}]]
    (tcp/tcp-server port handlers
                    :host host
                    :options options)))
