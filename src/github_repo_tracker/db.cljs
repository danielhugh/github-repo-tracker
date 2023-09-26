(ns github-repo-tracker.db
  (:require [cljs.reader]
            [malli.core :as m]
            [re-frame.core :as rf]))

(def distinct-sequence
  (m/schema [:and
             [:sequential any?]
             [:fn {:error/message "all elements should be distinct"}
              (fn [xs]
                (or (empty? xs)
                    (apply distinct? xs)))]]))

(def RepoList
  [:and [:vector string?] distinct-sequence])

(def RepositoryMetadata
  [:map
   [:viewed? boolean?]
   [:last-viewed-at inst?]])

(def Repository
  [:map
   [:id string?]
   [:description string?]
   [:name string?]
   [:nameWithOwner string?]
   [:url string?]
   [:latestRelease
    [:maybe
     [:map
      [:id string?]
      [:description string?]
      [:tagName string?]
      [:publishedAt string?]]]]])

(def AppErrors
  [:vector
   [:map
    [:message string?]]])

(def App
  [:map
   [:active-repo {:optional true} string?]
   [:adding-repo? boolean?]
   [:errors {:optional true} AppErrors]])

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
  (.setItem js/localStorage ls-key (str db)))

;; cofx Registrations  -----------------------------------------------------

(rf/reg-cofx
 ::local-store-data
 (fn [cofx _]
   (assoc cofx :local-store-data
          (into (hash-map)
                (some->> (.getItem js/localStorage ls-key)
                         (cljs.reader/read-string))))))
