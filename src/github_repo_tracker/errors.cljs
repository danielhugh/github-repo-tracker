(ns github-repo-tracker.errors
  (:require
   [github-repo-tracker.subs :as subs]
   [malli.core :as m]
   [malli.error :as me]
   [re-frame.core :refer [subscribe]]))

(defn error-component-ui []
  (let [error @(subscribe [::subs/first-app-error])]
    [:p.help.is-danger (:message error)]))

(defn humanize-errors
  "Given a malli `schema`, an atom `data` containing the data to validate
   against, and an atom `errors`, updates the atom `errors` with humanized
   validation results in the format returned by `malli.error/humanize`."
  [schema data errors]
  (reset! errors (me/humanize (m/explain schema @data))))

(defn error-message-ui
  [form-state error-state k]
  (let [error-msg (str (first (get error-state k)))]
    (when (and (contains? form-state k)
               (contains? error-state k))
      [:p.help.is-danger (str "Error: " error-msg)])))
