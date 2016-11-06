(ns dunbar-api.config
  (:require [environ.core :refer [env]]))

(defn load-config []
  env)

(defn postgres-uri [config]
  (:postgres-uri config))

(defn postgres-user [config]
  (:postgres-user config))

(defn postgres-password [config]
  (:postgres-password config))