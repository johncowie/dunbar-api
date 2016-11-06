(ns dunbar-api.test-utils
  (:require [dunbar-api.db :as db]
            [dunbar-api.handler :as h]
            [ring.mock.request :as mock]
            [cheshire.core :as json]
            [dunbar-api.config :as config]))

(defn with-db [f]
  (let [db (db/create-db (config/load-config))]
    (db/migrate-db db nil)
    (db/delete-all db)
    (f db)
    (db/stop-db db)))

(defn with-app [f]
  (let [db (db/create-db (config/load-config))
        app (h/app db)]
    (db/migrate-db db nil)
    (db/delete-all db)
    (f app)
    (db/stop-db db)))

(defn json-post-req [path body]
  (let [json (json/generate-string body)]
    (-> (mock/request :post path)
        (mock/body json)
        (mock/content-type "application/json; charset=utf-8"))))

