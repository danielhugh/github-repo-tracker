(ns github-repo-tracker.schema.core)

(def AppErrors
  [:vector
   [:map
    [:message string?]]])

(def App
  [:map
   [:active-repo {:optional true} string?]
   [:adding-repo? boolean?]
   [:errors {:optional true} AppErrors]])
