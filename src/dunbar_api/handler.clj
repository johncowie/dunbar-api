(ns dunbar-api.handler
  (:require [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [org.httpkit.server :refer [run-server]]
            [scenic.routes :refer [scenic-handler]]
            [dunbar-api.routes :as r]
            [dunbar-api.db :as db]
            [ring.util.response :refer [response content-type status]])
  (:gen-class))

(defn handlers [db]
  {:home (constantly (-> (response "hello world") (content-type "text/plain")))
   :create-friend (constantly (-> (response "created") (content-type "text/plain") (status 201)))})

(defn app
  "Takes a configuration map, a store object (i.e. to interact with the database),
     and a clock object (i.e. for timebased operations), and returns a ring request handler."
  [db]
  (-> (scenic-handler r/routes (handlers db))
      ;wrap-json-response
      ;(m/wrap-exceptions c/error-handler)
      (wrap-defaults api-defaults)
      ;(wrap-json-body {:keywords? true})
      ;(m/wrap-allow-access-all)
      ))

(defn -main [& args]
  (let [db (db/create-db)]
    (db/migrate-db db nil)
    (run-server (app db) {:port 8080 :host "0.0.0.0"})))