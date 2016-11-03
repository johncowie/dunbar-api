(ns dunbar-api.handler
  (:require [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [org.httpkit.server :refer [run-server]]
            [scenic.routes :refer [scenic-handler]]
            [dunbar-api.routes :as r]
            [ring.util.response :refer [response]]))

(defn handlers []
  {:home (constantly (response "hello world"))}
  )

(defn app
  "Takes a configuration map, a store object (i.e. to interact with the database),
     and a clock object (i.e. for timebased operations), and returns a ring request handler."
  []
  (-> (scenic-handler r/routes (handlers))
      ;wrap-json-response
      ;(m/wrap-exceptions c/error-handler)
      (wrap-defaults api-defaults)
      ;(wrap-json-body {:keywords? true})
      ;(m/wrap-allow-access-all)
      ))
