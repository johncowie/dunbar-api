(ns dunbar-api.db-test
  (:require [midje.sweet :refer :all]
            [dunbar-api.db :as db]
            [dunbar-api.test-utils :as u]
            [clj-time.core :as t]))

(u/with-db
  (fn [db]
    (facts "can insert and retrieve friend"
           (db/retrieve-friend db "bilbo") => nil
           (db/create-friend db {:firstName "bilbo"
                                 :lastName  "baggins"
                                 :id        "bilbo-baggins"
                                 :user      "john"})
           (db/retrieve-friend db "bilbo-baggins") => {:firstName "bilbo"
                                                       :lastName  "baggins"
                                                       :id        "bilbo-baggins"
                                                       :user      "john"})))

(u/with-db
  (fn [db]
    (facts "can insert, retrieve and delete user token"
           (let [john-token {:user   "john"
                             :token  "the random token"
                             :expiry (t/date-time 2016 11 7 14 50 30 200)}
                 bob-token {:user   "bob"
                            :token  "bobs token"
                            :expiry (t/date-time 2016 1 1 0 0 0 0)}]
             (db/retrieve-user-token db "john") => nil
             (db/retrieve-user-token db "bob") => nil
             (db/create-user-token db john-token)
             (db/create-user-token db bob-token)
             (db/retrieve-user-token db "john") => john-token
             (db/retrieve-user-token db "bob") => bob-token
             (db/delete-user-token db "john")
             (db/retrieve-user-token db "john") => nil
             (db/retrieve-user-token db "bob") => bob-token))))