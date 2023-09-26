(ns github-repo-tracker.core
  (:require
   [github-repo-tracker.config :as config]
   [github-repo-tracker.events :as events]
   [github-repo-tracker.views :as views]
   [re-frame.core :as rf]
   [reagent.dom :as rdom]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn ^:export init []
  (rf/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
