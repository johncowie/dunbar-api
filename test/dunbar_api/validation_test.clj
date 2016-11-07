(ns dunbar-api.validation-test
  (:require [midje.sweet :refer :all]
            [dunbar-api.validation :as v]
            [detox.translate :as t]
            [detox.core :as d]))

(defn string-of [n]
  (apply str (repeat n \x)))

(tabular
  (fact "about firstName validation"
        (let [result (-> ?data (d/validate v/first-name-validator))]
          (-> result :value first :type) => ?error))
  ?data ?error
  {} [:first-name :mandatory]
  {:firstName nil} [:first-name :not-nil]
  {:firstName 1} [:first-name :is-string]
  {:firstName ""} [:first-name :not-blank]
  {:firstName "   "} [:first-name :not-blank]
  ;; TODO not just punctuation
  {:firstName (string-of 50)} nil
  {:firstName (string-of 51)} [:first-name :length-less-than]
  )

(tabular
  (fact "about lastName validation"
        (let [result (-> ?data (d/validate v/last-name-validator))]
          (-> result :value first :type) => ?error))
  ?data ?error
  {} [:last-name :mandatory]
  {:lastName nil} [:last-name :not-nil]
  {:lastName 1} [:last-name :is-string]
  {:lastName ""} [:last-name :not-blank]
  {:lastName "   "} [:last-name :not-blank]
  ;; TODO not just punctuation
  {:lastName (string-of 50)} nil
  {:lastName (string-of 51)} [:last-name :length-less-than])

(tabular
  (fact "about unknown key validation"
        (let [result (-> ?data (d/validate v/unknown-keys-validator))]
          (-> result :value first :type) => ?error))
  ?data ?error
  {} nil
  {:firstName "" :lastName ""} nil
  {:firstName "" :bob ""} [:valid-keys]
  )

(facts "about whole friend validation"
       (-> {:unknown ""} (d/validate v/friend-validator))
       => {:result :error
           :value  [{:type        [:first-name :mandatory]
                     :value       nil
                     :constraints {}}
                    {:type        [:last-name :mandatory]
                     :value       nil
                     :constraints {}}
                    {:type        [:valid-keys]
                     :value       {:unknown ""}
                     :constraints {:valid-keys [:firstName :lastName]}}]}

       (-> {:firstName "Bob" :lastName "the Builder"}
           (d/validate v/friend-validator)) => {:result :success
                                                :value  {:firstName "Bob" :lastName "the Builder"}})

(facts "about translating friend errors"
       (-> v/friend-validator
           d/possible-errors
           (t/check-translations v/friend-translations)) => {:missing [] :superfluous []})

(tabular
  (fact "about login username validation"
        (let [user-exists-fn (fn [user] ?user-exists)
              result (-> ?data (d/validate (v/username-validator user-exists-fn)))]
          (if (v/success? result)
            (fact "success value" (-> result :value) => ?error)
            (fact "error value" (-> result :value first :type) => ?error))
          ))
  ?data ?user-exists ?error
  {} true [:username :mandatory]
  {:username nil} true [:username :not-nil]
  {:username 1} true [:username :is-string]
  {:username ""} true [:username :not-blank]
  {:username "  "} true [:username :not-blank]
  {:username (string-of 26)} true [:username :length-less-than]
  {:username (string-of 25)} true {:username (string-of 25)}
  {:username "geoff"} false [:username :user-exists]
  {:username (str "   " (string-of 25) "  ")} true {:username (string-of 25)}
  {:username " JoHn  100 "} true {:username "john100"})

(tabular
  (fact "about login password validation"
        (let [result (-> ?data (d/validate v/password-validator))]
          (if (v/success? result)
            (fact "success value" (-> result :value) => ?error)
            (fact "error value" (-> result :value first :type) => ?error))))
  ?data ?error
  {} [:password :mandatory]
  {:password nil} [:password :not-nil]
  {:password 1} [:password :is-string]
  {:password (string-of 51)} [:password :length-less-than]
  {:password (string-of 50)} {:password (string-of 50)})

(tabular
  (fact "about login username and password match validation"
        (let [password-check-fn (fn [u p] ?matches)
              result (-> ?data (d/validate (v/password-check-validator password-check-fn)))]
          (if (v/success? result)
            (fact "success value" (-> result :value) => ?error)
            (fact "error value" (-> result :value first :type) => ?error))))
  ?data ?matches ?error
  ;; {} true [:password-check :mandatory]                      ;; FIXME this doesn't happen - bug with traversy
  ;; {:username "blah"} true [:password-check :mandatory]      ;; FIXME this doesn't happen - bug with traversy
  {:username "u" :password "p"} false [:password-check :correct-password]
  {:username "u" :password "p"} true {:username "u" :password "p"}
  )

(tabular
  (fact "about whole login validation"
        (let [user-exists-fn (fn [u] true)
              password-check-fn (fn [u p] ?matches)
              result (-> ?data (d/validate (v/login-validator user-exists-fn password-check-fn)))]
          (if (v/success? result)
            (fact "success value" (-> result :value) => ?error)
            (fact "error value" (-> result :value first :type) => ?error))))
  ?data ?matches ?error
  {:password "p"} true [:username :mandatory]
  {:username "u"} true [:password :mandatory]
  {:username "u" :password "p"} false [:password-check :correct-password]
  {:username "u" :password "p"} true {:username "u" :password "p"}
  )

(facts "about translating login errors"
       (-> (v/login-validator nil nil)
           d/possible-errors
           (t/check-translations v/login-translations)) => {:missing [] :superfluous []})