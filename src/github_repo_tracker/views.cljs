(ns github-repo-tracker.views
  (:require [github-repo-tracker.errors :refer [error-component-ui]]
            [github-repo-tracker.events :as events]
            [github-repo-tracker.repository.views :refer [release-notes-ui
                                                          repo-list-ui
                                                          track-repo-form-ui]]
            [re-frame.core :refer [dispatch]]))

(defn main-panel []
  [:div.container.is-fluid
   [:header.my-4
    [:div.columns
     [:h1.title.column "GitHub Repo Tracker"]
     [:div.column.is-2
      [:button.button.is-danger.is-pulled-right
       {:on-click #(dispatch [::events/clear-app-data])}
       "Clear App Data"]]]]
   [:main
    [track-repo-form-ui]
    [error-component-ui]
    [:div.columns
     [:div.column.is-6
      [repo-list-ui]]
     [:div.column
      [release-notes-ui]]]]])
