(ns github-repo-tracker.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
 ::app-state
 (fn [db _]
   (get db :app)))

(reg-sub
 ::active-repo
 (fn [_]
   (subscribe [::app-state]))
 (fn [app-state _]
   (get app-state :active-repo)))

(reg-sub
 ::adding-repo?
 (fn [_]
   (subscribe [::app-state]))
 (fn [app-state _]
   (get app-state :adding-repo?)))

(reg-sub
 ::app-errors
 (fn [_]
   (subscribe [::app-state]))
 (fn [app-state _]
   (get app-state :errors)))

(reg-sub
 ::first-app-error
 (fn [_]
   (subscribe [::app-errors]))
 (fn [app-errors _]
   (first app-errors)))

(comment
  @(subscribe [::app-state])
  @(subscribe [::active-repo])
  @(subscribe [::adding-repo?])
  @(subscribe [::app-errors]))
