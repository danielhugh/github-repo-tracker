(ns github-repo-tracker.repository.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [github-repo-tracker.subs :as app-subs]))

;; Repository

(reg-sub
 ::repos
 (fn [db _]
   (get db :repos)))

(reg-sub
 ::repo-list
 (fn [db _]
   (get db :repo-list)))

(reg-sub
 ::repo-by-id
 (fn [db [_ id]]
   (get-in db [:repos id])))

(reg-sub
 ::repo-metadata-by-id
 (fn [db [_ id]]
   (get-in db [:repos id :metadata])))

(reg-sub
 ::repo-info-by-id
 (fn [db [_ id]]
   (get-in db [:repos id :repo-info])))

(reg-sub
 ::repo-viewed?
 (fn [[_ id]]
   (subscribe [::repo-metadata-by-id id]))
 (fn [repo-metadata _]
   (get repo-metadata :viewed?)))

(comment
  @(subscribe [::repos])
  @(subscribe [::repo-list])

  ;; microsoft/vscode
  @(subscribe [::repo-by-id "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="])
  @(subscribe [::repo-metadata-by-id "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="])
  @(subscribe [::repo-info-by-id "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="])
  @(subscribe [::repo-viewed? "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="]))

;; Releases

(reg-sub
 ::latest-release-by-id
 (fn [[_ id]]
   (subscribe [::repo-info-by-id id]))
 (fn [repo-info _]
   (get repo-info :latestRelease)))

(reg-sub
 ::latest-release-date-str-by-id
 (fn [[_ id]]
   (subscribe [::latest-release-by-id id]))
 (fn [latest-release]
   (when-let [published-at (:publishedAt latest-release)]
     (.toLocaleDateString (js/Date. published-at)))))

(reg-sub
 ::latest-release-notes-by-id
 (fn [[_ id] _]
   (subscribe [::latest-release-by-id id]))
 (fn [repo-info _]
   (get repo-info :description)))

(reg-sub
 ::latest-release-info
 (fn [_]
   [(subscribe [::app-subs/active-repo]) (subscribe [::repos])])
 (fn [[active-repo-id repos] _]
   (get-in repos [active-repo-id :repo-info :latestRelease])))

(comment
  @(subscribe [::latest-release-by-id "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="])
  @(subscribe [::latest-release-date-str-by-id "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="])
  @(subscribe [::latest-release-notes-by-id "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="])
  @(subscribe [::latest-release-info]))