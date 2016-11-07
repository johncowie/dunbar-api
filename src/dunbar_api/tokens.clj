(ns dunbar-api.tokens
  (:require [dunbar-api.db :as db]
            [dunbar-api.clock :as clock]
            [clj-time.core :as t]
            [dunbar-api.config :as config]
            [crypto.password.bcrypt :as password])
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

(defn add-token [login token now-dt config]
  (let [expiry (t/plus now-dt (t/seconds (config/token-expiry config)))]
    (-> login
        (assoc :token token #_(password/encrypt token))     ;; TODO only add encryption when story is played to verify token
        (assoc :expiry expiry))))

(defn expired? [token clock]
  (let [expiry (:expiry token)
        now-dt (clock/now-dt clock)]
    (t/after? now-dt expiry)))

(defn new-token [username db clock token-generator config]
  (let [token (generate-token token-generator)
        now-dt (clock/now-dt clock)
        token-data (-> {:user username} (add-token token now-dt config))]
    (db/create-user-token db token-data)
    token))

(defn get-token-for-user [username db clock token-generator config]
  (if-let [retrieved-token (db/retrieve-user-token db username)]
    (if (expired? retrieved-token clock)
      (do (db/delete-user-token db username)
          (new-token username db clock token-generator config))
      (:token retrieved-token))
    (new-token username db clock token-generator config)))
