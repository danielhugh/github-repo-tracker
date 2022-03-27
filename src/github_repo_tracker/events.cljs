(ns github-repo-tracker.events
  (:require
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
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

;; Helpers --------------------------------------------------------------------

(defn extract-repo [db]
  (let [repo-items (-> db :search-repo-response :items)
        repo (first repo-items)]
    (if (empty? repo-items)
      (assoc db :adding-repo? false)
      (let [repo (select-keys repo
                              [:id :full_name :description :html_url])]
        (assoc-in db [:repos (:id repo)] repo)))))

;; Event Handlers -------------------------------------------------------------

(rf/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
            db/default-db))

(rf/reg-event-fx
 ::search-repo
 [check-schema-interceptor]
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
 [check-schema-interceptor]
 (fn [{:keys [db]} [_ response]]
   {:db (assoc db :search-repo-response response)
    :fx [[:dispatch [::add-repo]]]}))

(rf/reg-event-db
 ::search-repo-failure
 [check-schema-interceptor]
 (fn [db _]
   (assoc db :adding-repo? false)))

(rf/reg-event-fx
 ::add-repo
 [check-schema-interceptor]
 (fn [{:keys [db]} _]
   {:db (extract-repo db)}))

(rf/reg-event-fx
 ::track-repo
 [check-schema-interceptor]
 (fn [{:keys [db]} [_ repo-name]]
   {:db (assoc db :adding-repo? true)
    :fx [[:dispatch [::search-repo repo-name]]]}))

(comment
  @re-frame.db/app-db

  ;; repos that exists
  (rf/dispatch [::track-repo "betterthantomorrow/calva"])
  (rf/dispatch [::track-repo "day8/re-frame"])
  (rf/dispatch [::track-repo "thheller/shadow-cljs"])

  ;; repos that does not exist
  (rf/dispatch [::track-repo "day8/calva"])
  )
