(ns dunbar-api.handler-test
  (:require [midje.sweet :refer :all]
            [dunbar-api.handler :as h]
            [ring.mock.request :as mock]))

(def app (h/app))

(facts "handler returns something"
       (-> (mock/request :get "/") app :body) => "hello world")
