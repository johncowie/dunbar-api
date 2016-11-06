(ns dunbar-api.handler
  (:require [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [org.httpkit.server :refer [run-server]]
            [scenic.routes :refer [scenic-handler]]
            [dunbar-api.routes :as r]
            [dunbar-api.db :as db]
            [dunbar-api.validation :as v]
            [dunbar-api.config :as config]
            [ring.util.response :refer [response content-type status]]
            [clojure.string :as str])
  (:gen-class))


(defn add-user [friend]
  (assoc friend :user "john"))

(def trimmed-lowercase (comp str/trim str/lower-case))

(defn gen-id [{:keys [firstName lastName] :as friend}]
  (let [id (str (trimmed-lowercase firstName) "-" (trimmed-lowercase lastName))]
    (assoc friend :id id)))

(defn create-friend [db]
  (fn [req]
    (let [val-result (v/validate-friend (:body req))]
      (if (v/success? val-result)
        (let [updated (-> (:value val-result) add-user gen-id)]
          (db/create-friend db updated)
          (-> (response {:status "created" :url (r/path-for :view-friend :id (:id updated))})
              (status 201)))
        (-> (response {:status "error" :errors (:value val-result)})
            (status 400))))))

(defn view-friend [db]
  (fn [req]
    (let [id (-> req :params :id)
          friend (db/retrieve-friend db id)]
      (when friend
        (-> friend
            (select-keys [:firstName :lastName])
            response
            (status 200))))))

(defn not-found [req]
  (-> (response {:status "resource not found"})
      (status 404)))

(defn handlers [db]
  {:home          (constantly (-> (response "hello world") (content-type "text/plain")))
   :create-friend (create-friend db)
   :view-friend   (view-friend db)})

(defn app
  "Takes a configuration map, a store object (i.e. to interact with the database),
     and a clock object (i.e. for timebased operations), and returns a ring request handler."
  [db]
  (-> (scenic-handler r/routes (handlers db) not-found)
      wrap-json-response
      ;(m/wrap-exceptions c/error-handler)
      (wrap-defaults api-defaults)
      (wrap-json-body {:keywords? true})
      ;(m/wrap-allow-access-all)
      ))

(defn -main [& args]
  (let [config (config/load-config)
        db (db/create-db config)]
    (db/migrate-db db nil)
    (run-server (app db) {:port 8080 :host "0.0.0.0"})))