(ns github-repo-tracker.db
  (:require [cljs.reader]
            [re-frame.core :as rf]))

(def app-db-schema
  [:map
   [:adding-repo? {:optional true} boolean?]
   [:search-repo-response {:optional true} [:map-of any? any?]]
   [:latest-release-response {:optional true} [:map-of any? any?]]
   [:repos [:map-of int?
            [:map
             [:description [:maybe string?]]
             [:full_name string?]
             [:html_url string?]
             [:id int?]
             [:latest-release {:optional true}
              [:map
               [:tag_name string?]
               [:published_at inst?]]]]]]])

(def default-db
  {:repos {}})

;; Local Storage  ----------------------------------------------------------

(def ls-key "github-repo-tracker")

(defn repos->local-store
  [{:keys [repos]}]
  (.setItem js/localStorage ls-key (str repos)))

;; cofx Registrations  -----------------------------------------------------

(rf/reg-cofx
 ::local-store-repos
 (fn [cofx _]
   (assoc cofx :local-store-repos
          (into (hash-map)
                (some->> (.getItem js/localStorage ls-key)
                         (cljs.reader/read-string))))))
