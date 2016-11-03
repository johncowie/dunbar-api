(ns dunbar-api.migrations
  (:require [ragtime.jdbc :as jdbc]
            [ragtime.repl :as ragtime]
            [clojure.tools.logging :as log]))

(defn migrations-config [conn]
  {:datastore  (jdbc/sql-database {:connection conn})
   :migrations (jdbc/load-resources "migrations")})

(defn take-migrations [ms migration-id]
  (take-while #(= migration-id (:id %)) ms)
  ;; FIXME this doesn't work
  )

;; FIXME write a test for this
(defn prune-migrations [migrations-config migration-id]
  (update migrations-config :migrations take-migrations migration-id))

(defn run-migrations
  ([conn migration-id]
   (let [m-config (migrations-config conn)]
     (if migration-id
       (do (log/debug (format "Migrating to id %s" migration-id))
           (ragtime/migrate (prune-migrations m-config migration-id)))
       (do (log/debug "Migration to latest id")
           (ragtime/migrate m-config)))))
  ([conn]
   (run-migrations conn nil)))

(defn run-rollback [conn migration-id]
  (let [config (migrations-config conn)]
    (if migration-id
      (ragtime/rollback config migration-id)
      (ragtime/rollback config))))

