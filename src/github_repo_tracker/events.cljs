(ns github-repo-tracker.events
  (:require
   [re-frame.core :as rf]
   [github-repo-tracker.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]))

(rf/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
            db/default-db))

(rf/reg-event-fx
 ::add-repo
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
 (fn [db [_ result]]
   (let [raw-repo (-> result :items first)
         repo (select-keys raw-repo
                           [:id :full_name :description])]
     (-> (assoc db :adding-repo? false)
         (assoc-in [:repos (:id repo)] repo)))))

(rf/reg-event-db
 ::add-repo-failure
 (fn [db [_ result]]
   ;; TODO
   db))

(rf/reg-event-db
 ::reset-db
 (fn [_ _]
   db/default-db))

(comment
  @re-frame.db/app-db

  (rf/dispatch [::reset-db])

  (rf/dispatch [::handler-with-http])
  (rf/dispatch [::handler-with-http])

  ;; repos that exists
  (rf/dispatch [::add-repo "betterthantomorrow/calva"])
  (rf/dispatch [::add-repo "day8/re-frame"])
  (rf/dispatch [::add-repo "thheller/shadow-cljs"])

  ;; repos that does not exist
  (rf/dispatch [::add-repo "day8/calva"]))
