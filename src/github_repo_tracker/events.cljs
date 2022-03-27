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

;; Event Handlers -------------------------------------------------------------

(rf/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
            db/default-db))

(rf/reg-event-fx
 ::add-repo
 [check-schema-interceptor]
 (fn [{:keys [db]} [_ repo-name]]
   {:db (assoc db :adding-repo? true)
    :fx [[:http-xhrio {:method :get
                       :uri "https://api.github.com/search/repositories"
                       :params {:q (str "repo:" repo-name)}
                       :timeout 5000
                       :response-format (ajax/json-response-format {:keywords? true})
                       :on-success [::add-repo-success]
                       :on-failure [::add-repo-failure]}]]}))

(rf/reg-event-db
 ::add-repo-success
 [check-schema-interceptor]
 (fn [db [_ result]]
   (let [raw-repo (-> result :items first)
         repo (select-keys raw-repo
                           [:id :full_name :description])]
     (cond-> (assoc db :adding-repo? false)
       (> (:total_count result) 0) (assoc-in [:repos (:id repo)] repo)))))

(rf/reg-event-db
 ::add-repo-failure
 [check-schema-interceptor]
 (fn [db [_ result]]
   ;; TODO
  db))

(comment
  @re-frame.db/app-db

  ;; repos that exists
  (rf/dispatch [::add-repo "betterthantomorrow/calva"])
  (rf/dispatch [::add-repo "day8/re-frame"])
  (rf/dispatch [::add-repo "thheller/shadow-cljs"])

  ;; repos that does not exist
  (rf/dispatch [::add-repo "day8/calva"])
  )
