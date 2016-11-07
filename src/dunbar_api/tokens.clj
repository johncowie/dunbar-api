(ns dunbar-api.tokens
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
