(ns github-repo-tracker.repository
  (:require
   [cljs.pprint :refer [pprint]]
   [graphql-builder.core :as core]
   [graphql-builder.parser :refer-macros [defgraphql]]
   [re-frame.core :as rf]
   [malli.core :as m]
   [reagent.core :as r]
   [re-graph.core :as re-graph]))

;; Setup

(defgraphql graphql-queries "graphql/repo.graphql")
(def query-map (graphql-builder.core/query-map graphql-queries))

(def repo-query (get-in query-map [:query :repo]))

;; Schema

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
    [:map
     [:id string?]
     [:tagName string?]
     [:publishedAt string?]]]])

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
        repos (get-in db [:new-schema :repos])]
    (cond-> db
      (not (repo-exists? repos repo-id))
      (update-in [:new-schema :repo-list]
                 append-id-to-repo-list repo-id)

      true
      (update-in [:new-schema :repos] add-repo repo-id repo-info))))

(defn- update-app-errors
  [app-state errors]
  (assoc app-state :errors errors))

(defn- gql-track-repo-handler-failure
  [db errors]
  (update-in db [:new-schema :app] update-app-errors errors))

(defn- set-request-loading
  [app-state adding-repo?]
  (assoc app-state :adding-repo? adding-repo?))

;; Events

(rf/reg-event-db
 ::gql-track-repo-handler
 [rf/debug]
 (fn [db [_ {:keys [response]}]]
   (let [{:keys [data errors]} response]
     (tap> response)
     ;; REVIEW: In GraphQL, failure is not total. Bother with partial handling?
     (cond
       (some? errors)
       (gql-track-repo-handler-failure db errors)

       (some? data)
       (gql-track-repo-handler-success db data)

       :else db))))

(rf/reg-event-fx
 ::gql-track-repo
 [rf/debug]
 (fn [{:keys [db]} [_ form]]
   (let [gql-info (repo-query form)]
     {:db (update-in db [:new-schema :app] set-request-loading true)
      :fx [[:dispatch [::re-graph/query
                       {:query (get-in gql-info [:graphql :query])
                        :variables (get-in gql-info [:graphql :variables])
                        :callback  [::gql-track-repo-handler]}]]]})))

;; Subs

(rf/reg-sub
 ::new-schema
 (fn [db _]
   (:new-schema db)))

;; Views

(defn graphql-form-ui
  []
  (r/with-let [draft (r/atom {})]
    [:form
     [:div.field
      [:label.label "Repository Owner"]
      [:div.control
       [:input.input
        {:type "text"
         :placeholder "e.g. microsoft"
         :on-change #(swap! draft assoc :owner (-> % .-target .-value))}]]]
     [:div.field
      [:label.label "Repository Name"]
      [:div.control
       [:input.input
        {:type "text"
         :placeholder "e.g. vscode"
         :on-change #(swap! draft assoc :name (-> % .-target .-value))}]]]
     [:pre (str @draft)]
     [:div.control
      [:button.button.is-primary
       {:type "submit"
        :on-click (fn [e]
                    (.preventDefault e)
                    (rf/dispatch [::gql-track-repo @draft]))}
       "Submit"]]
     [:pre
      (with-out-str (pprint @(rf/subscribe [::new-schema])))]]))
