(ns dunbar-api.handler-test
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [dunbar-api.handler :as h]
            [dunbar-api.routes :as r]
            ))

(def app (h/app))

(facts "handler returns something"
       (-> (mock/request :get "/") app :body) => "hello world")

(facts "there is a post friends route"
       (let [resp (-> (mock/request :post (r/path-for :create-friend)) app)]
         (:status resp) => 201))
