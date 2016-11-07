(ns dunbar-api.tokens
  (:require [dunbar-api.db :as db]
            [dunbar-api.clock :as clock]
            [dunbar-api.model.login :as login]
            [clj-time.core :as t])
  (:import (java.util UUID)))

(defprotocol TokenGenerator
  (generate-token [this]))

(deftype UUIDTokenGenerator []
  TokenGenerator
  (generate-token [this]
    (str (UUID/randomUUID))))

(defn create-uuid-token-generator []
  (UUIDTokenGenerator. ))

(deftype ListTokenGenerator [l]
  TokenGenerator
  (generate-token [this]
    (let [token (first @l)]
      (swap! l rest)
      token)))

(defn create-stub-token-generator [token-list]
  (ListTokenGenerator. (atom token-list)))

(defn add-token [login token now-dt]
  (let [expiry (t/plus now-dt (t/days 1))]                  ;; TODO configure duration
    (-> login
        (assoc :token token)
        (assoc :expiry expiry))))

(defn expired? [token clock]
  (let [expiry (:expiry token)
        now-dt (clock/now-dt clock)]
    (t/after? now-dt expiry)))

(defn new-token [username db clock token-generator]
  (let [token (generate-token token-generator)
        now-dt (clock/now-dt clock)
        token-data (-> {:user username} (add-token token now-dt))]
    (db/create-user-token db token-data)
    token))

(defn get-token-for-user [username db clock token-generator]
  (if-let [retrieved-token (db/retrieve-user-token db username)]
    (if (expired? retrieved-token clock)
      (do (db/delete-user-token db username)
          (new-token username db clock token-generator))
      (:token retrieved-token))
    (new-token username db clock token-generator)))
