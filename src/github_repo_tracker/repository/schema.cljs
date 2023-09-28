(ns github-repo-tracker.repository.schema
  (:require
   [github-repo-tracker.schema.utils :refer [distinct-sequence
                                             non-empty-string]]))

;; Models

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

;; Forms

(def track-repo-form-schema
  [:map
   [:owner non-empty-string]
   [:name non-empty-string]])
