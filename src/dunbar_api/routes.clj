(ns dunbar-api.routes
  (:require [scenic.routes :refer [load-routes-from-file]]
            [bidi.bidi :as b]))

(def routes (load-routes-from-file "routes.txt"))

;(ns imin-platform.api.routing
;  "Provides functions for dealing with url routing"
;  (:require [scenic.routes :refer [load-routes-from-file]]
;            [bidi.bidi :as b]
;            [imin-platform.config.env :as config]))
;
;(def routes (load-routes-from-file "routes.txt"))
;
(defn path-for
  "Given a handler key (i.e. as defined on the right-hand side of the routes in routes.txt), then return a url.
   Optionally accepts parameter key/value pairs for parameters in the path of the url"
  [handler-key & params]
  (if-let [path (apply b/path-for routes handler-key params)]
    path
    (throw (Exception. (format "Unknown path for handler-key %s" handler-key)))))
;
;(defn- absolute [path config]
;  (str (config/app-root-url config) path))
;
;(defn abs-path-for
;  "Returns an absolute url for a handler key, by looking up the app url from configuration and
;    adding it to the relative path."
;  [config handler-key & params]
;  (-> (apply path-for handler-key params)
;      (absolute config)))
