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

(defn get-token [app username password]
  (-> (u/json-post-req (r/path-for :login) {:username username :password password})
      app
      u/json-body
      :token))

(defn create-friend [app token first-name last-name]
  (-> (u/json-post-req (r/path-for :create-friend) {:firstName first-name :lastName last-name})
      (assoc-in [:headers "AuthToken"] token)
      app))

(defn has-body? [data]
  (fn [resp]
    (= (u/json-body resp) data)))

(defn is-friend? [first-name last-name]
  (has-body? {:firstName first-name :lastName last-name}))

(defn view-friend [app token path]
  (-> (u/json-get-req path)
      (assoc-in [:headers "AuthToken"] token)
      app))

(defn has-status? [status]
  (fn [resp] (= (:status resp) status)))

(facts "about creating friend resources"
       (u/with-app
         {:config {:username "john" :password "password"}}
         (fn [app]
           (let [token (get-token app "john" "password")]
             (fact "can not post friend without token"
                   (create-friend app nil "David" "Bowie") => (has-status? 401))
             (fact "can not post friend with invalid token"
                   (create-friend app "blah" "David" "Bowie") => (has-status? 401))
             (fact "can post and retrieve a friend"
                   (let [resp (create-friend app token "David" "Bowie")
                         resp-json (u/json-body resp)]
                     resp => (has-status? 201)
                     (fact "response has url to resource"
                           resp-json => (contains {:status "created"
                                                   :url    anything}))
                     (fact "resource url returns data"
                           (view-friend app token (:url resp-json)) => (every-checker (has-status? 200) (is-friend? "David" "Bowie")))
                     (fact "resource url returns 401 if token is incorrect"
                           (view-friend app "invalid-token" (:url resp-json)) => (has-status? 401))
                     (fact "resource url returns 404 if friend doesn't exist"
                           (view-friend app token (r/path-for :view-friend :id "blah")) => (has-status? 404))))
             (fact "returns 400 if friend already exists"
                   (let [resp (create-friend app token "David" "Bowie")]
                     resp => (has-status? 400)
                     resp => (has-body? {:status "error"
                                         :errors {:friend-not-exists "Friend already exists"}})))
             (fact "returns 400 for invalid friend"
                   (let [resp (create-friend app token "" "")]
                     resp => (has-status? 400)
                     resp => (has-body? {:status "error"
                                         :errors {:first-name {:not-blank "First name must not be blank"}
                                                  :last-name  {:not-blank "Last name must not be blank"}}})))))))


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
    (future-fact "full happy-path flow")
    )
  )
