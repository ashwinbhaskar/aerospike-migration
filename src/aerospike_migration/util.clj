(ns aerospike-migration.util
  (:require [clojure.walk :refer [postwalk]]
            [camel-snake-kebab.core :as csk]
            [clojure.string :as str])
  (:import
    (java.time Instant)))

(defn slice [v n]
  (let [length (count v)]
    (if (>= n length)
      [(into [] v)]
      (let [m (mod length n)]
        (conj
          (mapv #(into [] %) (partition n n v))
          (into [] (drop (- length m) v)))))))

(defn slice-lazy
  [lazy-seq batch-size no-of-lines]
  (if (>= batch-size no-of-lines)
    [lazy-seq]
    (let [m (mod no-of-lines batch-size)]
      (concat
        (partition batch-size batch-size lazy-seq)
        [(drop (- no-of-lines m) lazy-seq)]))))

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

(defn unit-type [u]
  (if (str/blank? u)
    "customer"
    u))
