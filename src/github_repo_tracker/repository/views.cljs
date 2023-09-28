(ns github-repo-tracker.repository.views
  (:require
   [github-repo-tracker.errors :refer [humanize-errors error-message-ui]]
   [github-repo-tracker.repository.events :as events]
   [github-repo-tracker.repository.schema :refer [track-repo-form-schema]]
   [github-repo-tracker.repository.subs :as subs]
   [github-repo-tracker.subs :as app-subs]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]))

(defn repo-item-ui [repo]
  (let [repo-id (:id repo)
        release-date-str @(subscribe [::subs/latest-release-date-str-by-id repo-id])
        up-to-date @(subscribe [::subs/repo-viewed? repo-id])
        selected-repo @(subscribe [::app-subs/active-repo])
        latest-release @(subscribe [::subs/latest-release-by-id repo-id])
        tag-name (:tagName latest-release)]
    [:<>
     [:article.media.columns.mt-4
      {:style (cond-> {}
                (= selected-repo repo-id)
                (conj {"backgroundColor" "#eeeeee"}))}
      [:figure.media-left.column.is-4
       [:div.tags.has-addons
        [:span.tag.is-dark (:nameWithOwner repo)]
        (when tag-name
          [:span.tag.is-info tag-name])]
       [:a {:href (:url repo) :target "_blank"} (:nameWithOwner repo)]]
      [:div.media-content
       [:div.content
        [:p (:nameWithOwner repo)]
        [:p (:description repo)]
        (when release-date-str
          [:p "Latest publish date: " release-date-str])
        (if up-to-date
          [:div
           [:span.icon.has-text-success
            [:i.fas.fa-check-circle]]
           [:span "You are up-to-date"]]
          [:div
           [:span.icon.has-text-info
            [:i.fas.fa-info-circle]]
           [:span "New release info!"]])
        [:button.button.is-info
         {:on-click #(dispatch [::events/view-release-notes repo-id])}
         "View Details"]]]
      [:div.media-right
       [:button.delete]]]]))

(defn repo-list-ui []
  (let [repo-list @(subscribe [::subs/repo-list])]
    [:div
     (doall
      (for [repo-id repo-list]
        (let [repo-info @(subscribe [::subs/repo-info-by-id repo-id])]
          ^{:key repo-id}
          [repo-item-ui repo-info])))]))

(defn release-notes-ui []
  (let [repo-list @(subscribe [::subs/repo-list])
        active-repo @(subscribe [::app-subs/active-repo])
        release-info @(subscribe [::subs/latest-release-info])]
    [:div
     [:h2.subtitle "Release Notes"]
     (cond
       (empty? repo-list)
       [:p "Add a repo to start viewing its release notes."]

       (nil? active-repo)
       [:p "Select a repo to view its release notes."]

       (empty? release-info)
       [:p "No release notes provided"]

       :else
       [:p (:description release-info)])]))

(defn track-repo-form-ui []
  (r/with-let [draft (r/atom {})
               validation-errors (r/atom {})
               form-schema track-repo-form-schema
               run-validation #(humanize-errors form-schema draft validation-errors)
               valid-form? #(and (nil? @validation-errors)
                                 (seq @draft))]
    [:form {:on-submit (fn [e]
                         (.preventDefault e)
                         (run-validation)
                         (when (valid-form?)
                           (dispatch [::events/gql-track-repo @draft])
                           (reset! draft {})
                           (reset! validation-errors {})))}
     [:div.field
      [:label.label "Repository Owner"]
      [:div.control
       [:input.input
        {:type "text"
         :placeholder "e.g. microsoft"
         :disabled @(subscribe [::app-subs/adding-repo?])
         :value (:owner @draft)
         :on-change #(do (swap! draft assoc :owner (-> % .-target .-value))
                         (run-validation))}]]
      [error-message-ui @draft @validation-errors :owner]]
     [:div.field
      [:label.label "Repository Name"]
      [:div.control
       [:input.input
        {:type "text"
         :placeholder "e.g. vscode"
         :disabled @(subscribe [::app-subs/adding-repo?])
         :value (:name @draft)
         :on-change #(do (swap! draft assoc :name (-> % .-target .-value))
                         (run-validation))}]]
      [error-message-ui @draft @validation-errors :name]]
     [:div.control
      [:button.button.is-primary
       {:type "submit"
        :disabled (or (not (valid-form?))
                      @(subscribe [::app-subs/adding-repo?]))}
       "Track Repo"]]]))
