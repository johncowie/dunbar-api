(ns dunbar-api.validation
  (:require [detox.core :as d]
            [detox.validators :as v]
            [detox.translate :as t]
            [detox.traversy :as tr]
            [dunbar-api.model.friend :as friend]))

(def name-validator (d/chain v/not-nil v/is-string v/not-blank (v/length-less-than 51)))

(def first-name-validator (tr/at name-validator :first-name friend/friend->first-name))
(def last-name-validator (tr/at name-validator :last-name friend/friend->last-name))

(def unknown-keys-validator (v/valid-keys friend/valid-keys))

(def friend-validator (d/group
                        first-name-validator
                        last-name-validator
                        unknown-keys-validator))

(def friend-translations {:first-name {:is-string        "First name must be a string"
                                       :length-less-than "First name must be less than ~~limit~~ characters"
                                       :not-blank        "First name must not be blank"
                                       :not-nil          "First name cannot be nil"}
                          :last-name  {:is-string        "Last name must be a string"
                                       :length-less-than "Last name must be less than ~~limit~~ characters"
                                       :not-blank        "Last name must not be blank"
                                       :not-nil          "Last name cannot be nil"}
                          :valid-keys "Data contains invalid keys"})

(defn validate-friend [friend]
  (-> friend
      (d/validate friend-validator)
      (t/translate friend-translations)))

(def success? d/success?)