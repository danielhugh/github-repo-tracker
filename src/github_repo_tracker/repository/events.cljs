(ns github-repo-tracker.repository.events
  (:require
   [github-repo-tracker.graphql :refer [repo-query]]
   [github-repo-tracker.interceptors :refer [standard-interceptors]]
   [re-frame.core :refer [reg-event-db reg-event-fx]]
   [re-graph.core :as re-graph]))

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
  [repo-list repo-id]
  (some #(= % repo-id) repo-list))

(defn- gql-track-repo-handler-success
  "Returns db with upserted repo info"
  [db data]
  (let [repo-id (extract-repo-id data)
        repo-info (extract-repo-info data)
        repo-list (get db :repo-list)]
    (cond-> db
      (not (repo-exists? repo-list repo-id))
      (update :repo-list
              append-id-to-repo-list repo-id)

      true
      (update :repos add-repo repo-id repo-info))))

(defn- update-app-errors
  [app-state errors]
  (assoc app-state :errors errors))

(defn- clear-app-errors
  [app-state]
  (dissoc app-state :errors))

(defn- gql-track-repo-handler-failure
  [db errors]
  (update db :app update-app-errors errors))

(defn- set-request-loading
  [app-state adding-repo?]
  (assoc app-state :adding-repo? adding-repo?))

(defn- set-repo-viewed-status
  [repo-metadata]
  (assoc repo-metadata
         :viewed? true
         :last-viewed-at (js/Date.)))

(defn- set-active-repo
  [app-state id]
  (assoc app-state :active-repo id))

;; Events

(reg-event-db
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
         (update :app set-request-loading false)))))

(reg-event-fx
 ::gql-track-repo
 [standard-interceptors]
 (fn [{:keys [db]} [_ form]]
   (let [gql-info (repo-query form)]
     {:db (-> db
              (update :app set-request-loading true)
              (update :app clear-app-errors))
      :fx [[:dispatch [::re-graph/query
                       {:query (get-in gql-info [:graphql :query])
                        :variables (get-in gql-info [:graphql :variables])
                        :callback  [::gql-track-repo-handler]}]]]})))

(reg-event-db
 ::view-release-notes
 [standard-interceptors]
 (fn [db [_ id]]
   (-> db
       (update :app set-active-repo id)
       (update-in [:repos id :metadata] set-repo-viewed-status))))
