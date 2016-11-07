(ns dunbar-api.migrations-test
  (:require [midje.sweet :refer :all]
            [dunbar-api.db :as db]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [dunbar-api.test-utils :as u]))

(defn adhoc-query-succeeds? [db query-str]
  (try
    (with-open [conn (.getConnection (:pool db))]
      (boolean (jdbc/query {:connection conn} [query-str])))
    (catch Exception e
      (println (.getMessage e))
      false)))

(defn column-exists? [db table-and-col-name]
  (let [[table-name column-name] (str/split table-and-col-name #"\.")
        query (format "SELECT %s FROM %s LIMIT 1;" column-name table-name)]
    (when (adhoc-query-succeeds? db query)
      table-and-col-name)))

(defn table-exists? [db table-name]
  (let [query (format "SELECT * FROM %s LIMIT 1;" table-name)]
    (when (adhoc-query-succeeds? db query)
      table-name)))

(u/with-db
  (fn [db]
    (facts "migration 001"
           (db/migrate-db db "001-initialise-tables")
           (facts "creates friends table and tokens table"
                  (table-exists? db "friends") => truthy
                  (column-exists? db "friends.user_id") => truthy
                  (column-exists? db "friends.id") => truthy
                  (column-exists? db "friends.first_name") => truthy
                  (column-exists? db "friends.last_name") => truthy
                  (table-exists? db "user_tokens") => truthy
                  (column-exists? db "user_tokens.user_id") => truthy
                  (column-exists? db "user_tokens.token") => truthy
                  (column-exists? db "user_tokens.expiry") => truthy
                  (db/rollback-db db nil)
                  (table-exists? db "friends") => falsey
                  (table-exists? db "user_tokens") => falsey
                  (future-fact "can call rollback with a specific ID - non-inclusive")
                  (future-fact "throws exception for non-existant migration id")
                  ;(m/run-migrations db)
                  (db/migrate-db db nil)
                  (db/stop-db db)
                  ))))
