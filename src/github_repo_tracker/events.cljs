(ns github-repo-tracker.events
  (:require
   [github-repo-tracker.config :as config]
   [github-repo-tracker.db :as db]
   [github-repo-tracker.interceptors :refer [standard-interceptors]]
   [re-frame.core :as rf]
   [re-graph.core :as re-graph]))

(rf/reg-event-fx
 ::initialize-db
 [standard-interceptors
  (rf/inject-cofx ::db/local-store-data)]
 (fn [{:keys [local-store-data]} [_ {:keys [reset?]
                                     :or {reset? false}
                                     :as _opts}]]
   {:db (merge db/default-db
               (if reset? {} local-store-data))
    :fx [[:dispatch [::re-graph/init
                     {:ws nil
                      :http {:url "https://api.github.com/graphql"
                             :impl {:with-credentials? false
                                    :headers {"Authorization" (str "Bearer " config/GITHUB_ACCESS_TOKEN)}}}}]]]}))
(rf/reg-event-fx
 ::clear-app-data
 [standard-interceptors]
 (fn [_ _]
   {:fx [[:dispatch [::initialize-db {:reset? true}]]]}))
