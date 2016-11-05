(ns dunbar-api.model.friend
  (:require [traversy.lens :as l]))

(def friend->first-name (l/in [:firstName]))
(def friend->last-name (l/in [:lastName]))
