(ns github-repo-tracker.db
  (:require [cljs.reader]
            [github-repo-tracker.repository.schema :refer [RepoList Repository
                                                           RepositoryMetadata]]
            [github-repo-tracker.schema.core :refer [App]]
            [re-frame.core :as rf]))

(def app-db-schema
  [:map
   [:app {:optional true} App]
   [:repo-list RepoList]
   [:repos [:map-of
            string? [:map
                     [:metadata {:optional true} RepositoryMetadata]
                     [:repo-info Repository]]]]])

(def default-db
  {:repo-list []
   :repos {}})

;; Local Storage  ----------------------------------------------------------

(def ls-key "github-repo-tracker")

(defn db->local-store
  [db]
  (.setItem js/localStorage ls-key (str (dissoc db :app))))

;; cofx Registrations  -----------------------------------------------------

(rf/reg-cofx
 ::local-store-data
 (fn [cofx _]
   (assoc cofx :local-store-data
          (into (hash-map)
                (some->> (.getItem js/localStorage ls-key)
                         (cljs.reader/read-string))))))
