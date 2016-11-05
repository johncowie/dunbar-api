(ns dunbar-api.model.friend
  (:require [traversy.lens :as l]))

(def valid-keys [:firstName :lastName])

(def friend->first-name (l/in [:firstName]))
(def friend->last-name (l/in [:lastName]))
