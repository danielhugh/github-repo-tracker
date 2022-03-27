(ns github-repo-tracker.db)

(def app-db-schema
  [:map
   [:adding-repo? {:optional true} boolean?]
   [:search-repo-response {:optional true} [:map-of any? any?]]
   [:repos [:map-of int?
            [:map
             [:description [:maybe string?]]
             [:full_name string?]
             [:html_url string?]
             [:id int?]]]]])

(def default-db
  {:repos {}})
