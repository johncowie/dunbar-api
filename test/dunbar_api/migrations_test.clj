(ns dunbar-api.migrations-test
  (:require [midje.sweet :refer :all]
            [dunbar-api.db :as db]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]))

(defn adhoc-query-succeeds? [connection-pool query-str]
  (try
    (with-open [conn (.getConnection connection-pool)]
      (boolean (jdbc/query {:connection conn} [query-str])))
    (catch Exception e
      (println (.getMessage e))
      false)))

(defn column-exists? [connection-pool table-and-col-name]
  (let [[table-name column-name] (str/split table-and-col-name #"\.")
        query (format "SELECT %s FROM %s LIMIT 1;" column-name table-name)]
    (when (adhoc-query-succeeds? connection-pool query)
      table-and-col-name)))

(defn table-exists? [connection-pool table-name]
  (let [query (format "SELECT * FROM %s LIMIT 1;" table-name)]
    (when (adhoc-query-succeeds? connection-pool query)
      table-name)))

(facts "migration 001"
       (let [db (db/create-db)
             pool (:pool db)]
         (db/migrate-db db "001-initialise-tables")
         (facts "creates friends table"
                (table-exists? pool "friends") => truthy
                (column-exists? pool "friends.user") => truthy
                (column-exists? pool "friends.id") => truthy
                (column-exists? pool "friends.first_name") => truthy
                (column-exists? pool "friends.last_name") => truthy
                (db/rollback-db db nil)
                (table-exists? pool "friends") => falsey
                (future-fact "can call rollback with a specific ID - non-inclusive")
                (future-fact "throws exception for non-existant migration id")
                ;(m/run-migrations db)
                (db/stop-db db)
                )))
