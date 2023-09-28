(ns github-repo-tracker.views
  (:require
   [github-repo-tracker.events :as events]
   [github-repo-tracker.repository :as repository]
   [re-frame.core :as rf]))

(defn main-panel []
  [:div.container.is-fluid
   [:header.my-4
    [:div.columns
     [:h1.title.column "GitHub Repo Tracker"]
     [:div.column.is-2
      [:button.button.is-danger.is-pulled-right
       {:on-click #(rf/dispatch [::events/clear-app-data])}
       "Clear App Data"]]]]
   [:main
    [repository/track-repo-form-ui]
    [repository/error-component-ui]
    [:div.columns
     [:div.column.is-6
      [repository/repo-list-ui]]
     [:div.column
      [repository/release-notes-ui]]]]])
