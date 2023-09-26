(ns github-repo-tracker.interceptors
  (:require
   [github-repo-tracker.config :as config]
   [github-repo-tracker.db :as db]
   [malli.core :as m]
   [malli.error :as me]
   [re-frame.core :as rf]))

(def valid-app-db?
  (m/validator db/app-db-schema))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the schema `schema`."
  [schema db]
  (when-not (valid-app-db? db)
    (throw
     (ex-info
      (str (-> schema
               (m/explain db)
               me/humanize))
      {}))))

(def check-schema-interceptor
  (rf/after (partial check-and-throw db/app-db-schema)))

(def ->local-store (rf/after db/db->local-store))

(def standard-interceptors
  [(when config/debug? rf/debug)
   (when config/debug? check-schema-interceptor)
   ->local-store])
