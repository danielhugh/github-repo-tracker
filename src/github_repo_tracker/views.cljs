(ns github-repo-tracker.views
  (:require
   [re-frame.core :as rf]
   [github-repo-tracker.subs :as subs]
   ))

(defn main-panel []
  (let [name (rf/subscribe [::subs/name])]
    [:div
     [:h1
      "Hello from " @name]
     ]))
