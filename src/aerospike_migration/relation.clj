(ns aerospike-migration.relation
  (:require [clojure.java.io :as jio]
            [failjure.core :as f]
            [aerospike-migration.util :as u]
            [next.jdbc :as j]
            [aerospike-migration.postgres :refer [ds]]))

(def mapping (atom {}))

(defn- column-name->bin-name
  [relation column-name]
  (-> @mapping
      relation
      ((keyword column-name))
      :bin))

(defn- column->bin
  [relation column-name value]
  {(column-name->bin-name relation column-name) value})

(defn- row->bin
  [row relation]
  (reduce (fn [acc [k v]]
            (into acc (column->bin relation k v)))
          {}
          row))

(defn- sanitize-row
  [row]
  (let [r-fn (fn [acc [k v]]
               (assoc acc k (u/pg-object->map v)))]
    (reduce r-fn {} row)))

(defn migrate-relation
  [relation]
  (let [query (u/prepare-query @mapping relation)]
    (->> (j/execute! ds [query])
         (map u/kebab-caseize)
         (map sanitize-row)
         (map #(row->bin % relation)))))

(defn migrate
  [relation edn-filepath batch-size]
  (with-open [failure-logger (jio/writer (format "%s-failed-rows" relation))]
    (f/attempt-all [mappings         (u/load-edn edn-filepath)
                    _                (reset! mapping mappings)
                    relation-mapping (relation mappings)]
      ())))
