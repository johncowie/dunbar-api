(ns dunbar-api.model.login
  (:require [traversy.lens :as l]
            [clj-time.core :as t]))

(def valid-keys [:username :password])

(def login->username (l/in [:username]))
(def login->password (l/in [:password]))
