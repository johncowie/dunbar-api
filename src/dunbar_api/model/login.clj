(ns dunbar-api.model.login
  (:require [traversy.lens :as l]))

(def valid-keys [:username :password])

(def login->username (l/in [:username]))
(def login->password (l/in [:password]))
