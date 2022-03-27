(ns github-repo-tracker.views
  (:require
   [re-frame.core :as rf]
   [github-repo-tracker.subs :as subs]
   [github-repo-tracker.events :as events]
   [reagent.core :as r]))

(defn repo-item [repo]
  (println repo)
  [:li
   [:div (:full_name repo)]
   [:div (:description repo)]])

(defn repo-list []
  (let [repos @(rf/subscribe [::subs/repos])]
    [:ul
     (for [repo repos]
       ^{:key (:id repo)}
       [repo-item repo])]))

(defn add-repo-form []
  (let [search-query (r/atom "")
        add-repo #(rf/dispatch [::events/track-repo @search-query])
        clear-search #(reset! search-query "")]
    (fn []
      [:div
       [:input {:type "text"
                :value @search-query
                :auto-focus true
                :placeholder "Add a repo"
                :on-change #(reset! search-query
                                    (-> % .-target .-value))
                :on-key-down #(case (.-keyCode %)
                                13 (add-repo)
                                27 (clear-search)
                                nil)}]
       [:div @search-query]
       [:button {:on-click add-repo} "Add"]])))

(defn main-panel []
  [:div
   [:h1 "GitHub Repo Tracker"]
   [add-repo-form]
   [repo-list]])
