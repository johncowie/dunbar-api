(ns dunbar-api.db
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :refer [defqueries]]
            [dunbar-api.migrations :as m])
  (:import (com.zaxxer.hikari HikariDataSource)))

(defqueries "queries.sql")

(defprotocol MigrateableDB
  (migrate-db [this migration-id])
  (rollback-db [this rollback-id]))
;
(defn hikari-pool [config]
  (let [ds (doto (HikariDataSource.)
             (.setDriverClassName "org.postgresql.Driver")
             (.setJdbcUrl (str "jdbc:" "postgresql://localhost:5432/dunbar" #_(config/postgres-uri config)))
             ;(.setUsername "username" #_(config/postgres-user config))
             ;(.setPassword "password" #_(config/postgres-password config))
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
  (sql-delete-all-friends! {} {:connection conn}))

(defprotocol FriendDB
  (create-friend [this friend])
  (retrieve-friend [this friend-id])
  (delete-all [this]))

(defrecord PostgresDB []
  component/Lifecycle
  (start [this]
    (-start this {} #_config))
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
    (with-pool this -delete-all)))

(defn create-db []
  (component/start (PostgresDB.)))

(defn stop-db [db]
  (component/stop db))
