(ns dunbar-api.db
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :refer [defqueries]]
            [clj-time.coerce :as tc]
            [dunbar-api.migrations :as m]
            [dunbar-api.config :as config])
  (:import (com.zaxxer.hikari HikariDataSource)))

(defqueries "queries.sql")

(defprotocol MigrateableDB
  (migrate-db [this migration-id])
  (rollback-db [this rollback-id]))

(defprotocol FriendDB
  (create-friend [this friend])
  (retrieve-friend [this friend-id])
  (delete-all [this])
  (create-user-token [this user-token])
  (retrieve-user-token [this user-id])
  (delete-user-token [this user-id])
  (retrieve-user-for-token [this token]))
;
(defn hikari-pool [config]
  (let [ds (doto (HikariDataSource.)
             (.setDriverClassName "org.postgresql.Driver")
             (.setJdbcUrl (str "jdbc:" (config/postgres-uri config)))
             (.setUsername (config/postgres-user config))
             (.setPassword (config/postgres-password config))
             )]
    ;(when-let [pool-size 10 (config/postgres-pool-size config)]
    ;  (.setMaximumPoolSize ds pool-size))
    ds))

(defn with-pool-multi
  ([store fs opts]
   (try
     (jdbc/with-db-transaction [tx (merge {:datasource (:pool store)} opts)]
                               (last (doall (map #(% tx) fs))))
     (catch Exception e
       (.printStackTrace e)
       (.printStackTrace (.getCause e))
       (throw e))))
  ([store fs]
   (with-pool-multi store fs {})))

(defn with-pool
  ([store f opts]
   (with-pool-multi store [f] opts))
  ([store f]
   (with-pool-multi store [f] {})))

(defn healthcheck-query [conn]
  (let [query "SELECT 1;"]
    (jdbc/query conn [query])))

(defn -start [db config]
  (log/info "Starting postgres connection..")
  (let [pool (hikari-pool config)]
    (log/info "Testing postgres connection..")
    (with-pool {:pool pool} healthcheck-query)
    (log/info "Started postgres connection..")
    (assoc db :pool pool)))

(defn -stop [db]
  (log/info "Stopping postgres connection..")
  (.close (-> db :pool)))

(defn -create-friend [friend]
  (fn [conn]
    (sql-create-friend<! friend {:connection conn})))

(defn translate-friend [row]
  (when row
    {:id        (:id row)
     :user      (:user_id row)
     :firstName (:first_name row)
     :lastName  (:last_name row)}))

(defn -retrieve-friend [friend-id]
  (fn [conn]
    (-> (sql-retrieve-friend {:id friend-id} {:connection conn})
        first
        translate-friend)))

(defn -delete-all [conn]
  (sql-delete-all-friends! {} {:connection conn})
  (sql-delete-all-tokens! {} {:connection conn}))

(defn -insert-user-token [user-token]
  (let [updated (update user-token :expiry tc/to-sql-time)]
    (fn [conn]
      (sql-create-user-token<! updated {:connection conn}))))

(defn -delete-user-token [user-id]
  (fn [conn]
    (sql-delete-user-token! {:user user-id} {:connection conn})))

(defn translate-user-token [row]
  (when row
    {:user   (:user_id row)
     :token  (:token row)
     :expiry (tc/from-sql-time (:expiry row))}))

(defn -retrieve-user-token [user-id]
  (fn [conn]
    (-> (sql-retrieve-user-token {:user user-id} {:connection conn})
        first
        translate-user-token)))

(defn -retrieve-user-for-token [token]
  (fn [conn]
    (-> (sql-retrieve-user-for-token {:token token} {:connection conn})
        first
        translate-user-token)))

(defrecord PostgresDB [config]
  component/Lifecycle
  (start [this]
    (-start this config))
  (stop [this]
    (-stop this))
  MigrateableDB
  (migrate-db [this migration-id]
    (with-open [conn (.getConnection (:pool this))]
      (m/run-migrations conn migration-id)))
  (rollback-db [this rollback-id]
    (with-open [conn (.getConnection (:pool this))]
      (m/run-rollback conn rollback-id)))
  FriendDB
  (create-friend [this friend]
    (with-pool this (-create-friend friend)))
  (retrieve-friend [this friend-id]
    (with-pool this (-retrieve-friend friend-id)))
  (delete-all [this]
    (with-pool this -delete-all))
  (create-user-token [this user-token]
    (with-pool this (-insert-user-token user-token)))
  (retrieve-user-token [this user-id]
    (with-pool this (-retrieve-user-token user-id)))
  (retrieve-user-for-token [this token]
    (with-pool this (-retrieve-user-for-token token)))
  (delete-user-token [this user-id]
    (with-pool this (-delete-user-token user-id))))

(defn create-db [config]
  (component/start (PostgresDB. config)))

(defn stop-db [db]
  (component/stop db))
