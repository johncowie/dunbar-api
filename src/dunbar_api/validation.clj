(ns dunbar-api.validation
  (:require [detox.core :as d]
            [detox.validators :as v]
            [detox.translate :as t]
            [detox.traversy :as tr]
            [detox.macros :as m]
            [dunbar-api.model.friend :as friend]
            [dunbar-api.model.login :as login]
            [dunbar-api.utils.string :as ustr]
            [clojure.string :as str]))

(def success? d/success?)

;; Friend validations

(def name-validator (d/chain v/not-nil v/is-string v/not-blank (v/length-less-than 51)))

(def first-name-validator (tr/at name-validator :first-name friend/friend->first-name))
(def last-name-validator (tr/at name-validator :last-name friend/friend->last-name))

(def unknown-keys-validator (v/valid-keys friend/valid-keys))

(declare friend-not-exists)
(m/defpredicate friend-not-exists [v friend-exists-fn]
                (not (friend-exists-fn v)))

(defn friend-validator [friend-exists-fn]
  (d/chain
    (d/group
      first-name-validator
      last-name-validator
      unknown-keys-validator)
    (friend-not-exists friend-exists-fn)))

(def friend-translations {:first-name {:is-string        "First name must be a string"
                                       :length-less-than "First name must be less than ~~limit~~ characters"
                                       :not-blank        "First name must not be blank"
                                       :not-nil          "First name cannot be nil"}
                          :last-name  {:is-string        "Last name must be a string"
                                       :length-less-than "Last name must be less than ~~limit~~ characters"
                                       :not-blank        "Last name must not be blank"
                                       :not-nil          "Last name cannot be nil"}
                          :valid-keys "Data contains invalid keys"
                          :friend-not-exists "Friend already exists"})

(defn validate-friend [friend friend-exists-fn]
  (-> friend
      (d/validate (friend-validator friend-exists-fn))
      (t/translate friend-translations)))

;; login validations
(declare user-exists trim-lower-case correct-password)

(m/defpredicate user-exists [v user-exists-fn] (user-exists-fn v))
(m/defpredicate correct-password [[u p] password-check-fn] (password-check-fn u p))
(m/defvalidator trim-lower-case [v] (d/success-value (-> v ustr/remove-white-space str/lower-case)))
;; FIXME need deftransformer

(defn username-validator [user-exists-fn]
  (-> (d/chain v/not-nil v/is-string v/not-blank trim-lower-case (v/length-less-than 26) (user-exists user-exists-fn))
      (tr/at :username login/login->username)))

(def password-validator
  (-> (d/chain v/not-nil v/is-string (v/length-less-than 51))
      (tr/at :password login/login->password)))

(defn password-check-validator [password-check-fn]
  (-> (correct-password password-check-fn)
      (tr/at :password-check login/login->username login/login->password)))

(defn login-validator [user-exists-fn password-check-fn]
  (d/chain
    (d/group (username-validator user-exists-fn) password-validator)
    (password-check-validator password-check-fn)))

(def login-translations {:password       {:is-string        "Password must be a string"
                                          :length-less-than "Password must be less than ~~limit~~ characters"
                                          :not-nil          "Password can not be blank"}
                         :username       {:is-string        "Username must be a string"
                                          :length-less-than "Username must be less than ~~limit~~ characters"
                                          :not-nil          "Username can not be blank"
                                          :not-blank        "username can not be blank"
                                          :trim-lower-case  "** this message was returned in error **"
                                          :user-exists      "User does not exist"}
                         :password-check {:correct-password "Username or password was incorrect"}})

(defn validate-login [login user-exists-fn password-check-fn]
  (-> login
      (d/validate (login-validator user-exists-fn password-check-fn))
      (t/translate login-translations)))