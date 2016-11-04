(ns dunbar-api.handler-test
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [dunbar-api.handler :as h]
            [dunbar-api.routes :as r]
            [cheshire.core :as json]
            [dunbar-api.db :as db]))

(def db (db/create-db))
(db/rollback-db db nil)
(db/migrate-db db nil)

(def app (h/app db))

(facts "handler returns something"
       (-> (mock/request :get "/") app :body) => "hello world")

(facts "can post and retrieve a friend"
       (let [body (json/generate-string {:firstName "David" :lastName "Bowie"})
             req (-> (mock/request :post (r/path-for :create-friend))
                     (mock/body body)
                     (mock/content-type "application/json; charset=utf-8"))]
         (let [resp (-> req app)]
           (:status resp) => 201
           (future-fact "response has url to resource")
           (future-fact "resource url returns data")
           (db/retrieve-friend db "david-bowie") => {:firstName "David"
                                                     :lastName  "Bowie"
                                                     :user      "john"
                                                     :id        "david-bowie"}
           )
         )
       )

(db/stop-db db)
