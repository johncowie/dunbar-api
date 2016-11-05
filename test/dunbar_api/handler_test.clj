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
         (let [resp (-> req app)
               resp-json (json/parse-string (:body resp) keyword)]
           (:status resp) => 201
           (fact "response has url to resource"
                 resp-json => (contains {:status "created"
                                         :url    anything}))
           (fact "resource url returns data"
                 (let [req (mock/request :get (or (:url resp-json) "blah"))
                       resp (-> req app)
                       resp-json (when (:body resp) (json/parse-string (:body resp) keyword))]
                   (:status resp) => 200
                   resp-json => {:firstName "David"
                                 :lastName  "Bowie"})))))

(facts "returns 404 for friend that doesn't exist"
       (let [req (mock/request :get (r/path-for :view-friend :id "blah"))
             resp (-> req app)]
         (:status resp) => 404))

(db/stop-db db)
