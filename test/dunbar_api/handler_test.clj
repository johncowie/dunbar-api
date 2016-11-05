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

;; TODO move to test util
(defn json-post-req [path body]
  (let [json (json/generate-string body)]
    (-> (mock/request :post path)
        (mock/body json)
        (mock/content-type "application/json; charset=utf-8"))))

(facts "can post and retrieve a friend"
       (let [friend {:firstName "David" :lastName "Bowie"}
             req (json-post-req (r/path-for :create-friend) friend)
             resp (-> req app)
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
                 resp-json => friend))))

(facts "returns 400 for invalid friend"
       (let [req (json-post-req (r/path-for :create-friend) {:firstName "" :lastName "" :random "random"})
             resp (-> req app)
             resp-json (json/parse-string (:body resp) keyword)]
         (:status resp) => 400
         resp-json => {:status "error"
                       :errors {:first-name {:not-blank "First name must not be blank"}
                                :last-name {:not-blank "Last name must not be blank"}
                                :valid-keys "Data contains invalid keys"}}))

(facts "returns 404 for friend that doesn't exist"
       (let [req (mock/request :get (r/path-for :view-friend :id "blah"))
             resp (-> req app)]
         (:status resp) => 404))

(db/stop-db db)
