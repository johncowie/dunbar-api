(ns dunbar-api.clock
  (:require [clj-time.core :as t]))

(defprotocol Clock
  (now-dt [this]))

(defprotocol AdjustableClock
  (adjust [this f]))

(deftype JodaClock []
  Clock
  (now-dt [this]
    (t/now)))

(defn create-joda-clock []
  (JodaClock. ))

(deftype StubClock [dt-state]
  Clock
  (now-dt [this]
    @dt-state)
  AdjustableClock
  (adjust [this f]
    (swap! dt-state f)))

(defn create-adjustable-clock [dt]
  (StubClock. (atom dt)))