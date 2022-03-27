(ns github-repo-tracker.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::repos
 (fn [db]
   (vals (:repos db))))
