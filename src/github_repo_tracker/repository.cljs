(ns github-repo-tracker.repository
  (:require
   [cljs.pprint :refer [pprint]]
   [github-repo-tracker.interceptors :refer [standard-interceptors]]
   [graphql-builder.core :as core]
   [graphql-builder.parser :refer-macros [defgraphql]]
   [re-frame.core :as rf]
   [re-graph.core :as re-graph]
   [reagent.core :as r]
   [github-repo-tracker.db :as db]))

;; Setup

(defgraphql graphql-queries "graphql/repo.graphql")
(def query-map (graphql-builder.core/query-map graphql-queries))

(def repo-query (get-in query-map [:query :repo]))

;; Helpers

(defn- append-id-to-repo-list
  [repo-list repo-id]
  ((fnil conj []) repo-list repo-id))

(defn- add-repo
  [repos-m repo-id repo-info]
  (assoc-in repos-m [repo-id :repo-info] repo-info))

(defn- extract-repo-info
  [data]
  (get data :repository))

(defn- extract-repo-id
  [data]
  (get-in data [:repository :id]))

(defn- repo-exists?
  [repos repo-id]
  (some #(= % repo-id) repos))

(defn- gql-track-repo-handler-success
  "Returns db with upserted repo info"
  [db data]
  (let [repo-id (extract-repo-id data)
        repo-info (extract-repo-info data)
        repo-list (get-in db [:new-schema :repo-list])]
    (cond-> db
      (not (repo-exists? repo-list repo-id))
      (update-in [:new-schema :repo-list]
                 append-id-to-repo-list repo-id)

      true
      (update-in [:new-schema :repos] add-repo repo-id repo-info))))

(defn- update-app-errors
  [app-state errors]
  (assoc app-state :errors errors))

(defn- clear-app-errors
  [app-state]
  (dissoc app-state :errors))

(defn- gql-track-repo-handler-failure
  [db errors]
  (update-in db [:new-schema :app] update-app-errors errors))

(defn- set-request-loading
  [app-state adding-repo?]
  (assoc app-state :adding-repo? adding-repo?))

(defn- set-repo-viewed-status
  [repo-metadata]
  (assoc repo-metadata :viewed? true))

(defn- set-active-repo
  [app-state id]
  (assoc app-state :active-repo id))

;; Events

(rf/reg-event-db
 ::gql-track-repo-handler
 [standard-interceptors]
 (fn [db [_ {:keys [response]}]]
   (let [{:keys [data errors]} response]
     (tap> response)
     ;; REVIEW: In GraphQL, failure is not total. Bother with partial handling?
     (-> (cond
           (some? errors)
           (gql-track-repo-handler-failure db errors)

           (some? data)
           (gql-track-repo-handler-success db data)

           :else db)
         (update-in [:new-schema :app] set-request-loading false)))))

(rf/reg-event-fx
 ::gql-track-repo
 [standard-interceptors]
 (fn [{:keys [db]} [_ form]]
   (let [gql-info (repo-query form)]
     {:db (-> db
              (update-in [:new-schema :app] set-request-loading true)
              (update-in [:new-schema :app] clear-app-errors))
      :fx [[:dispatch [::re-graph/query
                       {:query (get-in gql-info [:graphql :query])
                        :variables (get-in gql-info [:graphql :variables])
                        :callback  [::gql-track-repo-handler]}]]]})))

(rf/reg-event-db
 ::view-release-notes
 [standard-interceptors]
 (fn [db [_ id]]
   (-> db
       (update-in [:new-schema :app] set-active-repo id)
       (update-in [:new-schema :repos id :metadata] set-repo-viewed-status))))

;; Subs

;;; App

(rf/reg-sub
 ::app-state
 (fn [db _]
   (get-in db [:new-schema :app])))

(rf/reg-sub
 ::active-repo
 (fn [_]
   (rf/subscribe [::app-state]))
 (fn [app-state _]
   (get app-state :active-repo)))

(rf/reg-sub
 ::adding-repo?
 (fn [_]
   (rf/subscribe [::app-state]))
 (fn [app-state _]
   (get app-state :adding-repo?)))

(rf/reg-sub
 ::app-errors
 (fn [_]
   (rf/subscribe [::app-state]))
 (fn [app-state _]
   (get app-state :errors)))

(rf/reg-sub
 ::first-app-error
 (fn [_]
   (rf/subscribe [::app-errors]))
 (fn [app-errors _]
   (first app-errors)))

(comment
  @(rf/subscribe [::app-state])
  @(rf/subscribe [::active-repo])
  @(rf/subscribe [::adding-repo?])
  @(rf/subscribe [::app-errors]))

;;; Repo

(rf/reg-sub
 ::new-schema
 (fn [db _]
   (:new-schema db)))

(rf/reg-sub
 ::repos
 (fn [db _]
   (get-in db [:new-schema :repos])))

(rf/reg-sub
 ::repo-list
 (fn [db _]
   (get-in db [:new-schema :repo-list])))

(rf/reg-sub
 ::repo-by-id
 (fn [db [_ id]]
   (get-in db [:new-schema :repos id])))

(rf/reg-sub
 ::repo-metadata-by-id
 (fn [db [_ id]]
   (get-in db [:new-schema :repos id :metadata])))

(rf/reg-sub
 ::repo-info-by-id
 (fn [db [_ id]]
   (get-in db [:new-schema :repos id :repo-info])))

(rf/reg-sub
 ::repo-viewed?
 (fn [[_ id]]
   (rf/subscribe [::repo-metadata-by-id id]))
 (fn [repo-metadata _]
   (get repo-metadata :viewed?)))

(comment
  @(rf/subscribe [::repos])
  @(rf/subscribe [::repo-list])

  ;; microsoft/vscode
  @(rf/subscribe [::repo-by-id "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="])
  @(rf/subscribe [::repo-metadata-by-id "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="])
  @(rf/subscribe [::repo-info-by-id "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="])
  ;; TODO: Test once view tag is reimplemented
  @(rf/subscribe [::repo-viewed? "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="]))

;;; Releases

(rf/reg-sub
 ::latest-release-by-id
 (fn [[_ id]]
   (rf/subscribe [::repo-info-by-id id]))
 (fn [repo-info _]
   (get repo-info :latestRelease)))

(rf/reg-sub
 ::latest-release-date-str-by-id
 (fn [[_ id]]
   (rf/subscribe [::latest-release-by-id id]))
 (fn [latest-release]
   (when-let [published-at (:publishedAt latest-release)]
     (.toLocaleDateString (js/Date. published-at)))))

(rf/reg-sub
 ::latest-release-notes-by-id
 (fn [[_ id] _]
   (rf/subscribe [::latest-release-by-id id]))
 (fn [repo-info _]
   (get repo-info :description)))

(rf/reg-sub
 ::latest-release-info
 (fn [_]
   [(rf/subscribe [::active-repo]) (rf/subscribe [::repos])])
 (fn [[active-repo-id repos] _]
   (get-in repos [active-repo-id :repo-info :latestRelease])))

(comment
  @(rf/subscribe [::latest-release-by-id "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="])
  @(rf/subscribe [::latest-release-date-str-by-id "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="])
  @(rf/subscribe [::latest-release-notes-by-id "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="])
  @(rf/subscribe [::latest-release-info]))

;; Views

(defn error-component-ui []
  (let [error @(rf/subscribe [::first-app-error])]
    [:p.help.is-danger (:message error)]))

(defn repo-item-ui [repo]
  (let [repo-id (:id repo)
        release-date-str @(rf/subscribe [::latest-release-date-str-by-id repo-id])
        up-to-date @(rf/subscribe [::repo-viewed? repo-id])
        selected-repo @(rf/subscribe [::active-repo])
        latest-release @(rf/subscribe [::latest-release-by-id repo-id])
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
       [:a {:href (:html_url repo) :target "_blank"} (:nameWithOwner repo)]]
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
         {:on-click #(rf/dispatch [::view-release-notes repo-id])}
         "View Details"]]]
      [:div.media-right
       [:button.delete]]]
     [:pre (with-out-str (pprint repo))]]))

(defn repo-list-ui []
  (let [repo-list @(rf/subscribe [::repo-list])]
    [:div
     (doall
      (for [repo-id repo-list]
        (let [repo-info @(rf/subscribe [::repo-info-by-id repo-id])]
          ^{:key repo-id}
          [repo-item-ui repo-info])))]))

(defn release-notes-ui []
  (let [repo-list @(rf/subscribe [::repo-list])
        active-repo @(rf/subscribe [::active-repo])
        release-info @(rf/subscribe [::latest-release-info])]
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

(defn graphql-form-ui []
  (r/with-let [draft (r/atom {})]
    [:form
     [:div.field
      [:label.label "Repository Owner"]
      [:div.control
       [:input.input
        {:type "text"
         :placeholder "e.g. microsoft"
         :value (:owner @draft)
         :on-change #(swap! draft assoc :owner (-> % .-target .-value))}]]]
     [:div.field
      [:label.label "Repository Name"]
      [:div.control
       [:input.input
        {:type "text"
         :placeholder "e.g. vscode"
         :value (:name @draft)
         :on-change #(swap! draft assoc :name (-> % .-target .-value))}]]]
     [:pre (str @draft)]
     [:div.control
      [:button.button.is-primary
       {:type "submit"
        :on-click (fn [e]
                    (.preventDefault e)
                    (rf/dispatch [::gql-track-repo @draft])
                    (reset! draft {}))}
       "Submit"]]
     [:pre
      (with-out-str (pprint @(rf/subscribe [::new-schema])))]]))
