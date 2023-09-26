(ns github-repo-tracker.events
  (:require
   [github-repo-tracker.db :as db]
   [github-repo-tracker.env :as env]
   [github-repo-tracker.interceptors :refer [standard-interceptors]]
   [re-graph.core :as re-graph]
   [re-frame.core :as rf]))

;; Event Handlers -------------------------------------------------------------

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
                     {:ws nil #_{:impl {:headers {:Authorization GITHUB-ACCESS-TOKEN}}}
                      :http {:url "https://api.github.com/graphql"
                             :impl {:with-credentials? false
                                    :headers {"Authorization" (str "Bearer " env/GITHUB-ACCESS-TOKEN)}}}}]]]}))

;; Search ---------------------------------------------------------------------

(rf/reg-event-fx
 ::clear-app-data
 [standard-interceptors]
 (fn [_ _]
   {:fx [[:dispatch [::initialize-db {:reset? true}]]]}))
