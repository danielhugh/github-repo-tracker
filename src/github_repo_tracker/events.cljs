(ns github-repo-tracker.events
  (:require
   [re-frame.core :as rf]
   [github-repo-tracker.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   ))

(rf/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   db/default-db))
