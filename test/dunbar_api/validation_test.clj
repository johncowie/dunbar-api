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

(future-facts "about unknown key validation"

              )

(facts "about whole friend validation"
       (-> {} (d/validate v/friend-validator)) => {:result :error
                                                   :value  [{:type        [:first-name :mandatory]
                                                             :value       nil
                                                             :constraints {}}
                                                            {:type        [:last-name :mandatory]
                                                             :value       nil
                                                             :constraints {}}]}
       (-> {:firstName "Bob" :lastName "the Builder"}
           (d/validate v/friend-validator)) => {:result :success
                                                :value  {:firstName "Bob" :lastName "the Builder"}})

(facts "about translating errors"
       (-> v/friend-validator
           d/possible-errors
           (t/check-translations v/friend-translations)) => {:missing [] :superfluous []})
