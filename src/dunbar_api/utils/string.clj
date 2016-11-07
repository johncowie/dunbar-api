(ns dunbar-api.utils.string
  (:require [clojure.string :as str]))

(defn remove-white-space [s]
  (str/replace s #"\s+" ""))
