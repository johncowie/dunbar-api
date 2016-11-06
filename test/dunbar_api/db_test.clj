(ns dunbar-api.db-test
  (:require [midje.sweet :refer :all]
            [dunbar-api.db :as db]
            [dunbar-api.test-utils :as u]))

(u/with-db
  (fn [db]
    (facts "can insert and retrieve friend"
           (db/rollback-db db nil)
           (db/migrate-db db nil)
           (db/retrieve-friend db "bilbo") => nil
           (db/create-friend db {:firstName "bilbo"
                                 :lastName  "baggins"
                                 :id        "bilbo-baggins"
                                 :user      "john"})
           (db/retrieve-friend db "bilbo-baggins") => {:firstName "bilbo"
                                                       :lastName  "baggins"
                                                       :id        "bilbo-baggins"
                                                       :user      "john"}
           (db/rollback-db db nil)
           (db/stop-db db))))