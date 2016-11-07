(ns dunbar-api.handler
  (:require [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [org.httpkit.server :refer [run-server]]
            [scenic.routes :refer [scenic-handler]]
            [dunbar-api.routes :as r]
            [dunbar-api.db :as db]
            [dunbar-api.clock :as clock]
            [dunbar-api.tokens :as token]
            [dunbar-api.validation :as v]
            [dunbar-api.config :as config]
            [dunbar-api.model.login :as login]
            [ring.util.response :refer [response content-type status]]
            [clojure.string :as str]
            [dunbar-api.utils.string :as ustr]
            [traversy.lens :as l])
  (:gen-class))


(defn add-user [friend]
  (assoc friend :user "john"))

(defn gen-id [{:keys [firstName lastName] :as friend}]
  (let [id (-> (str firstName "-" lastName) str/lower-case ustr/remove-white-space)]
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

(defn user-exists-fn [config]
  (fn [user]
    (= user (config/username config))))

(defn password-check-fn [config]
  (fn [user password]
    (and (= user (config/username config))
         (= password (config/password config)))))

(defn login [config db clock token-generator]
  (fn [req]
    (let [val-result (-> req :body (v/validate-login (user-exists-fn config) (password-check-fn config)))
          val-data (:value val-result)]
      (if (v/success? val-result)
        (let [user (l/view-single val-data login/login->username)
              token (token/get-token-for-user user db clock token-generator config)]
          (response {:status "success" :token token}))
        (-> (response {:status "error" :errors val-data})
            (status 400))))))

(defn not-found [req]
  (-> (response {:status "resource not found"})
      (status 404)))

(defn handlers [config db clock token-generator]
  {:home          (constantly (-> (response "hello world") (content-type "text/plain")))
   :create-friend (create-friend db)
   :view-friend   (view-friend db)
   :login         (login config db clock token-generator)})

(defn app
  "Takes a configuration map, a store object (i.e. to interact with the database),
     and a clock object (i.e. for timebased operations), and returns a ring request handler."
  [config db clock token-generator]
  (let [handler (handlers config db clock token-generator)]
    (-> (scenic-handler r/routes handler not-found)
        wrap-json-response
        ;(m/wrap-exceptions c/error-handler)
        (wrap-defaults api-defaults)
        (wrap-json-body {:keywords? true})
        ;(m/wrap-allow-access-all)
        )))

(defn -main [& args]
  (let [config (config/load-config)
        db (db/create-db config)
        clock (clock/create-joda-clock)
        token-generator (token/create-uuid-token-generator)]
    (db/migrate-db db nil)
    (run-server (app config db clock token-generator) {:port 8080 :host "0.0.0.0"})))