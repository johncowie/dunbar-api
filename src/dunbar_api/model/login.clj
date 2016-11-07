(ns dunbar-api.model.login
  (:require [traversy.lens :as l]))

(def valid-keys [:username :password])

(def login->username (l/in [:username]))
(def login->password (l/in [:password]))

(defn get-user [login]
  {:user (l/view-single login login->username)})

(defn add-token [login]
  (let [token nil
        expiry nil]
    (-> login
        (assoc :token token)
        (assoc :expiry expiry))))
