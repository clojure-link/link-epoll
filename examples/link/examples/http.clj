(ns link.examples.http
  (:require [link.http.epoll :as http]))

(defn app [req]
  {:body "<h1>It works</h1>"})

(defn -main [& args]
  (println "Starting http server")
  (http/http-server 8080 app :options {:tcp-fastopen (int 256)})
  (println "Http server started on 8080"))
