(ns aerospike-migration.util
  (:require [clojure.walk :refer [postwalk]]
            [camel-snake-kebab.core :as csk]
            [failjure.core :as f]
            [jsonista.core :as j]
            [clojure.string :as str])
  (:import
    (java.time Instant)
    (java.io PushbackReader)
    (org.postgresql.util PGobject)
    (java.sql Timestamp)))

(def mapper (j/object-mapper))

(defn pg-object->map
  [v]
  (if (instance? PGobject v)
    (let [type  (.getType v)
          value (.getValue v)]
      (if (#{"jsonb" "json"} type)
        (with-meta (j/read-value value mapper) {:pgtype type})
        value))
    v))

(defn now-utc-unix []
  (-> (Instant/now)
      .getEpochSecond))

(defn kebab-caseize
  [map]
  (->> map
       (postwalk (fn [a]
                   (if (keyword? a)
                     (csk/->kebab-case-keyword a)
                     a)))))

(defn load-edn
  [file-path]
  (-> (with-open [r (clojure.java.io/reader file-path)]
        (clojure.edn/read (PushbackReader. r)))
      (f/try*)))

(defn if-not-blank
  "Applies the function fn on s if s is not blank"
  [s fn]
  (if (str/blank? s)
    s
    (fn s)))
