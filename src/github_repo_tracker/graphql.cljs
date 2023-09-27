(ns github-repo-tracker.graphql
  (:require
   [graphql-builder.core :as core]
   [graphql-builder.parser :refer-macros [defgraphql]]))

;; Setup

(defgraphql graphql-queries "graphql/repo.graphql")

(def query-map (graphql-builder.core/query-map graphql-queries))

;; Queries

(def repo-query (get-in query-map [:query :repo]))
