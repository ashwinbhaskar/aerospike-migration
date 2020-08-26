(ns segmentation-migration.segments
  (:require [segmentation-migration.aerospike :refer [client]]
            [aerospike-clj.client :as aero]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [segmentation-migration.util :as u])
  (:import (java.util.function BiConsumer)
           (java.util.concurrent CompletableFuture)))

(defn- bool->int
  [v]
  (if v
    1
    0))
(defn- int->bool
  [v]
  (= v 1))

(defn- line->segments-bins
  [line]
  (let [[name created-at deleted unit-type] (s/split line #",")]
    {"segment"    name
     "unit_type"  (if (s/blank? unit-type)
                    "customer"
                    unit-type)
     "deleted"    (-> deleted
                      Boolean/valueOf
                      bool->int)
     "doc_type"   "s"
     "created_at" created-at}))

(defn- doc-key
  [unit-type segment-name]
  (format "segment:%s:%s" unit-type segment-name))

(defn- bi-consumer
  [map success-logger failure-logger]
  (reify BiConsumer
    (accept [this result exception]
      (let [{:strs [segment unit_type deleted created_at]} map]
        (if (not (nil? exception))
          (-> failure-logger
              (.write (format "%s,%s,%b,%s,%s\n" segment unit_type (int->bool deleted) created_at exception)))
          (-> success-logger
              (.write (format "%s,%s,%b,%s\n" segment unit_type (int->bool deleted) created_at))))))))

(defn- migrate
  [lines set-name success-logger failure-logger]
  (->> lines
       (map (fn [line]
              (let [m (line->segments-bins line)
                    doc-key (doc-key (get m "segment") (get m "unit_type"))]
                (-> (aero/put client doc-key set-name m -1)
                    (.whenCompleteAsync (bi-consumer m success-logger failure-logger))))))
       (into-array CompletableFuture)
       CompletableFuture/allOf
       .join))

(defn migrate-segments
  "Expects a csv file with columns in order - segment-name, created-at, deleted, unit-type"
  [csv-file batch-size]
  (with-open [sw (clojure.java.io/writer "/Users/ashwinbhaskar/Desktop/segmentation-migration/segment-migration-success.txt" :append true)
              fw (clojure.java.io/writer "/Users/ashwinbhaskar/Desktop/segmentation-migration/segment-migration-failure.txt" :append true)]
    (with-open [reader (io/reader csv-file)]
      (->> (u/slice (line-seq reader) batch-size)
           (run! #(migrate %  "segment" sw fw))))))
