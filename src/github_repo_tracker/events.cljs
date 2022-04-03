(ns github-repo-tracker.events
  (:require
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [github-repo-tracker.db :as db]
   [malli.core :as m]
   [malli.error :as me]
   [re-frame.core :as rf]))

;; Interceptors ---------------------------------------------------------------

(def valid-app-db?
  (m/validator db/app-db-schema))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the schema `schema`."
  [schema db]
  (when-not (valid-app-db? db)
    (throw
     (ex-info
      (str (-> schema
               (m/explain db)
               me/humanize))
      {}))))

(def check-schema-interceptor
  (rf/after (partial check-and-throw db/app-db-schema)))

(def ->local-store (rf/after db/repos->local-store))

;; Helpers --------------------------------------------------------------------

(defn extract-repo [db]
  (let [repo-items (-> db :search-repo-response :items)
        repo (first repo-items)]
    (if (empty? repo-items)
      (assoc db :adding-repo? false)
      (let [repo (merge {:viewed? false}
                        (select-keys repo
                                     [:id :full_name :description :html_url]))]
        (assoc-in db [:repos (:id repo)] repo)))))

(defn extract-release-info [release-response]
  (-> (select-keys release-response [:tag_name :published_at :body])
      (update :published_at #(cljs.reader/parse-timestamp %))))

;; Event Handlers -------------------------------------------------------------

(rf/reg-event-fx
 ::initialize-db
 [(rf/inject-cofx ::db/local-store-repos)
  check-schema-interceptor]
 (fn [{:keys [local-store-repos]} _]
   {:db (assoc db/default-db :repos local-store-repos)}))

;; Search ---------------------------------------------------------------------

(rf/reg-event-fx
 ::search-repo
 [check-schema-interceptor
  ->local-store]
 (fn [_ [_ repo-name]]
   {:fx [[:http-xhrio {:method :get
                       :uri "https://api.github.com/search/repositories"
                       :params {:q (str "repo:" repo-name)}
                       :timeout 5000
                       :response-format (ajax/json-response-format {:keywords? true})
                       :on-success [::search-repo-success]
                       :on-failure [::search-repo-failure]}]]}))

(rf/reg-event-fx
 ::search-repo-success
 [check-schema-interceptor
  ->local-store]
 (fn [{:keys [db]} [_ response]]
   {:db (assoc db :search-repo-response response)
    :fx [[:dispatch [::add-repo]]
         [:dispatch [::fetch-latest-release]]]}))

(rf/reg-event-db
 ::search-repo-failure
 [check-schema-interceptor
  ->local-store]
 (fn [db _]
   (assoc db :adding-repo? false)))

(rf/reg-event-fx
 ::add-repo
 [check-schema-interceptor
  ->local-store]
 (fn [{:keys [db]} _]
   {:db (extract-repo db)}))

(rf/reg-event-fx
 ::track-repo
 [check-schema-interceptor
  ->local-store]
 (fn [{:keys [db]} [_ repo-name]]
   {:db (assoc db :adding-repo? true)
    :fx [[:dispatch [::search-repo repo-name]]]}))

(rf/reg-event-fx
 ::clear-app-data
 [check-schema-interceptor
  ->local-store]
 (fn [_ _]
   {:db db/default-db}))

;; Releases -------------------------------------------------------------------

(rf/reg-event-fx
 ::fetch-latest-release
 (fn [{:keys [db]} _]
   [check-schema-interceptor
    ->local-store]
   (let [id (-> db :search-repo-response :items first :id)
         repo-full-name (-> db :search-repo-response :items first :full_name)]
     {:fx [[:http-xhrio {:method :get
                         :uri (str "https://api.github.com/repos/" repo-full-name "/releases/latest")
                         :timeout 5000
                         :response-format (ajax/json-response-format {:keywords? true})
                         :on-success [::fetch-latest-release-success id]
                         :on-failure [::fetch-latest-release-failure]}]]})))

(rf/reg-event-fx
 ::fetch-latest-release-success
 [check-schema-interceptor
  ->local-store]
 (fn [{:keys [db]} [_ id response]]
   {:db (assoc db :latest-release-response response)
    :fx [[:dispatch [::add-release-info-by-id id]]]}))

(rf/reg-event-fx
 ::add-release-info-by-id
 [check-schema-interceptor
  ->local-store]
 (fn [{:keys [db]} [_ id]]
   (let [release-response (:latest-release-response db)]
     {:db (-> db
              (assoc-in [:repos id :latest-release]
                        (extract-release-info release-response))
              (assoc :adding-repo? false))})))

(rf/reg-event-db
 ::fetch-latest-release-failure
 [check-schema-interceptor
  ->local-store]
 (fn [db _]
   (assoc db :adding-repo? false)))

;; Repos ----------------------------------------------------------------------

(rf/reg-event-fx
 ::select-repo
 [check-schema-interceptor
  ->local-store]
 (fn [{:keys [db]} [_ id]]
   {:db (assoc db :active-repo id)
    :fx [[:dispatch [::mark-repo-as-viewed id]]]}))

(rf/reg-event-db
 ::mark-repo-as-viewed
 [check-schema-interceptor
  ->local-store]
 (fn [db [_ id]]
   (assoc-in db [:repos id :viewed?] true)))

(comment
  @re-frame.db/app-db

  ;; repos that exists
  (rf/dispatch [::track-repo "betterthantomorrow/calva"])
  (rf/dispatch [::track-repo "day8/re-frame"])
  (rf/dispatch [::track-repo "thheller/shadow-cljs"])

  ;; repos that does not exist
  (rf/dispatch [::track-repo "day8/calva"]))
