(ns github-repo-tracker.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::repos
 (fn [db]
   (vals (:repos db))))

(rf/reg-sub
 ::repo-by-id
 (fn [db [_ id]]
   (get-in db [:repos id])))

(rf/reg-sub
 ::latest-release-date-str-by-id
 (fn [[_ id] _]
   (rf/subscribe [::repo-by-id id]))

 (fn [repo [_ id]]
   (when-let [published-at (get-in repo [:latest-release :published_at])]
     (.toLocaleDateString (js/Date. published-at)))))

(comment
  ;; babashka
  @(rf/subscribe [::repo-by-id 201467090])
  @(rf/subscribe [::latest-release-date-str-by-id 201467090])

  ;; calva
  @(rf/subscribe [::repo-by-id 125431277])
  @(rf/subscribe [::latest-release-date-str-by-id 125431277])

  ;; shadow-cljs (no releases)
  @(rf/subscribe [::repo-by-id 43973779])
  @(rf/subscribe [::latest-release-date-str-by-id 43973779])
  )
