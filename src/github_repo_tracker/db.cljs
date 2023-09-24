(ns github-repo-tracker.db
  (:require [cljs.reader]
            [github-repo-tracker.repository :refer [RepoList RepositoryMetadata Repository]]
            [re-frame.core :as rf]))

(def AppErrors
  [:vector
   [:map
    [:message string?]]])

(def App
  [:map
   [:active-repo {:optional true} string?]
   [:adding-repo? boolean?]
   [:errors {:optional true} AppErrors]])

(def app-db-schema2
  [:map
   [:app {:optional true} App]
   [:repo-list RepoList]
   [:repos [:map-of
            string? [:map
                     [:metadata {:optional true} RepositoryMetadata]
                     [:repo-info Repository]]]]])

(def app-db-schema
  [:map
   [:adding-repo? {:optional true} boolean?]
   [:search-repo-response {:optional true} [:map-of any? any?]]
   [:latest-release-response {:optional true} [:map-of any? any?]]
   [:repos [:map-of int?
            [:map
             [:description [:maybe string?]]
             [:full_name string?]
             [:html_url string?]
             [:id int?]
             [:latest-release {:optional true}
              [:map
               [:tag_name string?]
               [:published_at inst?]
               [:body {:optional true} string?]]]
             [:viewed? boolean?]
             [:last-viewed-at {:optional true} inst?]]]]
   [:active-repo {:optional true} int?]
   [:repo/error {:optional true} string?]
   [:new-schema app-db-schema2]])

(def default-db
  {:new-schema {:repo-list []
                :repos {}}
   :repos {}})

;; Local Storage  ----------------------------------------------------------

(def ls-key "github-repo-tracker")

(defn repos->local-store
  [{:keys [repos]}]
  (.setItem js/localStorage ls-key (str repos)))

;; cofx Registrations  -----------------------------------------------------

(rf/reg-cofx
 ::local-store-repos
 (fn [cofx _]
   (assoc cofx :local-store-repos
          (into (hash-map)
                (some->> (.getItem js/localStorage ls-key)
                         (cljs.reader/read-string))))))
