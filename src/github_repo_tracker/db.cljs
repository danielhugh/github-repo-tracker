(ns github-repo-tracker.db)

(def app-db-schema
  [:map
   [:adding-repo? {:optional true} boolean?]
   [:repos [:map-of int?
            [:map
             [:description [:maybe string?]]
             [:full_name string?]
             [:id int?]]]]])

(def default-db
  {:repos {}})
