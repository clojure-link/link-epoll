(ns link.tcp.epoll
  (:require [link.core :refer :all]
            [link.codec :refer [netty-encoder netty-decoder]]
            [link.tcp :as tcp]
            [clojure.tools.logging :as logging])
  (:import [java.net InetAddress InetSocketAddress]
           [io.netty.bootstrap Bootstrap ServerBootstrap]
           [io.netty.channel ChannelInitializer Channel ChannelHandler
            ChannelHandlerContext ChannelFuture EventLoopGroup
            ChannelPipeline ChannelOption]
           [io.netty.channel.epoll EpollEventLoopGroup
            EpollServerSocketChannel EpollSocketChannel]
           [io.netty.util.concurrent EventExecutorGroup GenericFutureListener]
           [link.core ClientSocketChannel]))

(extend-protocol LinkMessageChannel
  EpollSocketChannel
  (id [this]
    (channel-id this))
  (send! [this msg]
    (.writeAndFlush this msg (.voidPromise this)))
  (send!* [this msg cb]
    (if cb
      (let [cf (.writeAndFlush this msg)]
        (.addListener ^ChannelFuture cf (reify GenericFutureListener
                                          (operationComplete [this f] (cb f)))))
      (.writeAndFlush this msg (.voidPromise this))))
  (channel-addr [this]
    (.localAddress this))
  (remote-addr [this]
    (.remoteAddress this))
  (close! [this]
    (.close this))
  (valid? [this]
    (.isActive this)))

(defn- start-tcp-server [host port handlers options]
  (let [boss-group (EpollEventLoopGroup.)
        worker-group (EpollEventLoopGroup.)
        bootstrap (ServerBootstrap.)

        channel-initializer (tcp/channel-init handlers)

        options (group-by #(.startsWith (name (% 0)) "child.") (into [] options))
        parent-options (get options false)
        child-options (map #(vector (keyword (subs (name (% 0)) 6)) (% 1)) (get options true))]
    (doto bootstrap
      (.group boss-group worker-group)
      (.channel EpollServerSocketChannel)
      (.childHandler channel-initializer))
    (doseq [op parent-options]
      (.option bootstrap (tcp/to-channel-option (op 0)) (op 1)))
    (doseq [op child-options]
      (.childOption bootstrap (tcp/to-channel-option (op 0)) (op 1)))

    (.sync ^ChannelFuture (.bind bootstrap (InetAddress/getByName host) port))
    ;; return event loop groups so we can shutdown the server gracefully
    [worker-group boss-group]))

(defn tcp-server [port handlers
                  & {:keys [options host]
                     :or {options {}
                          host "0.0.0.0"}}]
  (let [handlers (if (sequential? handlers) handlers [handlers])]
    (start-tcp-server host
                      port
                      handlers
                      options)))

(defn tcp-client-factory [handlers
                          & {:keys [options]
                             :or {options {}}}]
  (let [worker-group (EpollEventLoopGroup.)
        bootstrap (Bootstrap.)
        handlers (if (sequential? handlers) handlers [handlers])

        channel-initializer (tcp/channel-init handlers)
        options (into [] options)]

    (doto bootstrap
      (.group worker-group)
      (.channel EpollSocketChannel)
      (.handler channel-initializer))
    (doseq [op options]
      (.option bootstrap (tcp/to-channel-option (op 0)) (op 1)))

    [bootstrap worker-group]))
