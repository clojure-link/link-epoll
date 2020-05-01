(ns link.tcp.epoll
  (:require [link.core :refer :all]
            [link.codec :refer [netty-encoder netty-decoder]]
            [link.tcp :as tcp]
            [clojure.tools.logging :as logging]
            [clojure.string :as string])
  (:import [java.net InetAddress InetSocketAddress]
           [io.netty.bootstrap Bootstrap ServerBootstrap]
           [io.netty.channel ChannelInitializer Channel ChannelHandler
                             ChannelHandlerContext ChannelFuture EventLoopGroup
                             ChannelPipeline ChannelOption]
           [io.netty.channel.epoll EpollEventLoopGroup EpollServerSocketChannel
            EpollSocketChannel EpollChannelOption]
           [io.netty.util.concurrent EventExecutorGroup GenericFutureListener DefaultThreadFactory]
           [link.core ClientSocketChannel]
           (io.netty.util.internal SystemPropertyUtil)
           (io.netty.util NettyRuntime)))

(defn to-channel-option-with-epoll
  ([co]
   (to-channel-option-with-epoll co nil))
  ([co clazz]
   (let [co (name co)]
     (if (or (string/starts-with? co "epoll.")
             (string/starts-with? co "child.epoll."))
       (let [co (-> co
                    (string/replace-first #"epoll\.|child\.epoll\." "")
                    (string/replace #"-" "_")
                    (string/upper-case))]
         (if clazz
           (EpollChannelOption/valueOf ^Class clazz co)
           (EpollChannelOption/valueOf EpollChannelOption co)))
       (tcp/to-channel-option co clazz)))))

(extend-protocol LinkMessageChannel
  EpollSocketChannel
  (id [this]
    (channel-id this))
  (short-id [this]
    (short-channel-id this))
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

(defn- default-epoll-boss-group []
  (let [group (EpollEventLoopGroup. 1 (DefaultThreadFactory. "link-epoll-boss-group"))]
    (.setIoRatio group 100)
    group))

(defn- default-epoll-worker-group []
  (EpollEventLoopGroup. (max 1 (SystemPropertyUtil/getInt "io.netty.eventLoopThreads"
                                                          (* 2 (NettyRuntime/availableProcessors))))
                        (DefaultThreadFactory. "link-epoll-worker-group")))

(defn- start-tcp-server [host port handlers options]
  (let [boss-group (or (:boss-group options) (default-epoll-boss-group))
        worker-group (or (:worker-group options) (default-epoll-worker-group))
        bootstrap (or (:bootstrap options) (ServerBootstrap.))

        channel-initializer (tcp/channel-init handlers)

        options (->> (dissoc options :boss-group :worker-group :bootstrap)
                     (into [])
                     (group-by #(.startsWith (name (% 0)) "child.")))
        parent-options (get options false)
        child-options (map #(vector (keyword (subs (name (% 0)) 6)) (% 1)) (get options true))]
    (doto bootstrap
      (.group boss-group worker-group)
      (.channel EpollServerSocketChannel)
      (.childHandler channel-initializer))
    (doseq [op parent-options]
      (let [op (flatten op)]
        (.option bootstrap (apply to-channel-option-with-epoll (butlast op))
                 (last op))))
    (doseq [op child-options]
      (let [op (flatten op)]
        (.childOption bootstrap (apply to-channel-option-with-epoll (butlast op))
                      (last op))))

    (.sync ^ChannelFuture (.bind bootstrap (InetAddress/getByName host) port))
    ;; return event loop groups so we can shutdown the server gracefully
    [worker-group boss-group]))

(defn server-bootstrap
  "Allow multiple server instance share the same eventloop:
  Just use the result of this function as option in `tcp-server`"
  []
  {:boss-group (default-epoll-boss-group)
   :worker-group (default-epoll-worker-group)
   :boostrap (ServerBootstrap.)})

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
  (let [worker-group (or (:worker-group options) (default-epoll-worker-group))
        bootstrap (Bootstrap.)
        handlers (if (sequential? handlers) handlers [handlers])

        channel-initializer (tcp/channel-init handlers)
        options (->> (dissoc options :worker-group)
                     (into []))]
    (doto bootstrap
      (.group worker-group)
      (.channel EpollSocketChannel)
      (.handler channel-initializer))

    (doseq [op options]
      (.option bootstrap (to-channel-option-with-epoll (op 0)) (op 1)))

    [bootstrap worker-group]))
