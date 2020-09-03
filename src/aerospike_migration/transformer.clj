(ns aerospike-migration.transformer
  (:import (java.sql Timestamp)
           (java.text SimpleDateFormat)))

(defn timestamp->epoch-second
  [^Timestamp ts]
  (-> (.toInstant ts)
      (.getEpochSecond)))

(defn timestamp->string
  [^Timestamp ts format]
  (-> (SimpleDateFormat. format)
      (.format ts)))


(def functions {:identity                 identity
                :timestamp->epoch-seconds timestamp->epoch-second
                :->string                 str
                :timestamp->string        timestamp->string})
