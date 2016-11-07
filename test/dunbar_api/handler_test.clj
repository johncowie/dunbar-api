(ns dunbar-api.handler-test
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [dunbar-api.routes :as r]
            [cheshire.core :as json]
            [dunbar-api.test-utils :as u]
            [dunbar-api.clock :as clock]
            [clj-time.core :as t]
            [dunbar-api.tokens :as token]))

(u/with-app
  (fn [app]
    (facts "handler returns something"
           (-> (mock/request :get "/") app :body) => "hello world")))

(u/with-app
  (fn [app]
    (facts "can post and retrieve a friend"
           (let [friend {:firstName "David" :lastName "Bowie"}
                 req (u/json-post-req (r/path-for :create-friend) friend)
                 resp (-> req app)
                 resp-json (u/json-body resp)]
             (:status resp) => 201
             (fact "response has url to resource"
                   resp-json => (contains {:status "created"
                                           :url    anything}))
             (fact "resource url returns data"
                   (let [req (mock/request :get (or (:url resp-json) "blah"))
                         resp (-> req app)
                         resp-json (u/json-body resp)]
                     (:status resp) => 200
                     resp-json => friend))))))

(let [clock (clock/create-adjustable-clock (t/date-time 2016 1 1 0 0 0))
      token-gen (token/create-stub-token-generator ["t1" "t2" "t3"])]
  (u/with-app
    {:clock           clock
     :token-generator token-gen
     :config          {:username "john" :password "password"}}
    (fn [app]
      (fact "can login user and retrieve token"
            (let [login-details {:username "john" :password "password"}
                  resp (-> (u/json-post-req (r/path-for :login) login-details) app)
                  resp-json (u/json-body resp)]
              (:status resp) => 200
              resp-json => (contains {:status "success"
                                      :token  "t1"})))
      (fact "if password is incorrect, returns error response"
            (let [login-details {:username "john" :password "doh"}
                  resp (-> (u/json-post-req (r/path-for :login) login-details) app)
                  resp-json (u/json-body resp)]
              (:status resp) => 400
              resp-json => (contains {:status "error"})))
      (fact "if user is incorrect, returns error response"
            (let [login-details {:username "bob" :password "password"}
                  resp (-> (u/json-post-req (r/path-for :login) login-details) app)
                  resp-json (u/json-body resp)]
              (:status resp) => 400
              resp-json => (contains {:status "error"}))))))

(let [token-gen (token/create-stub-token-generator ["t1" "t2"])
      clock (clock/create-adjustable-clock (t/date-time 2016 1 1))]
  (u/with-db
    (fn [db]
      (facts "tokens are persisted between app life-cycles, but expired after a certain amount of time"
             (u/with-app
               {:db              db
                :token-generator token-gen
                :clock           clock
                :config          {:token-expiry 2 :username "darth" :password "luke"}}
               (fn [app]
                 (-> (u/json-post-req (r/path-for :login) {:username "darth" :password "luke"})
                     app u/json-body) => {:status "success" :token "t1"}))
             (u/with-app
               {:db              db
                :token-generator token-gen
                :clock           clock
                :config          {:token-expiry 2 :username "darth" :password "luke"}}
               (fn [app]
                 (-> (u/json-post-req (r/path-for :login) {:username "darth" :password "luke"})
                     app u/json-body) => {:status "success" :token "t1"}
                 (fact "after some time has elapsed, a new token is returned"
                       (clock/adjust clock #(t/plus % (t/seconds 3)))
                       (-> (u/json-post-req (r/path-for :login) {:username "darth" :password "luke"})
                           app u/json-body) => {:status "success" :token "t2"})))))))

(u/with-app
  (fn [app]
    (facts "returns 400 for invalid friend"
           (let [req (u/json-post-req (r/path-for :create-friend) {:firstName "" :lastName "" :random "random"})
                 resp (-> req app)
                 resp-json (json/parse-string (:body resp) keyword)]
             (:status resp) => 400
             resp-json => {:status "error"
                           :errors {:first-name {:not-blank "First name must not be blank"}
                                    :last-name  {:not-blank "Last name must not be blank"}
                                    :valid-keys "Data contains invalid keys"}}))))

(u/with-app
  (fn [app]
    (facts "returns 404 for friend that doesn't exist"
           (let [req (mock/request :get (r/path-for :view-friend :id "blah"))
                 resp (-> req app)]
             (:status resp) => 404))))

(u/with-app
  (fn [app]
    (future-fact "full happy-path flow")
    )
  )
