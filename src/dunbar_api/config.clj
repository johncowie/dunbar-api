(ns dunbar-api.config
  (:require [environ.core :refer [env]]))

(defn parse-int [s]
  (when s (Integer/parseInt s)))

(defn load-config []
  (update env :token-expiry parse-int))

(defn postgres-uri [config]
  (:postgres-uri config))

;; seconds
(defn token-expiry [config]
  (or (:token-expiry config) (* 60 24 24)))

(defn postgres-user [config]
  (:postgres-user config))

(defn postgres-password [config]
  (:postgres-password config))

(defn username [config]
  (:username config))

(defn password [config]
  (:password config))