(ns aerospike-migration.spec
  (:require [clojure.spec.alpha :as spec]
            [clojure.spec.alpha :as s]))

(spec/def ::columns (spec/and sequential?
                              #(every? keyword? %)))

(spec/def ::function #{:identity :timestamp->epoch-seconds})

(spec/def ::bin string?)

(spec/def ::column (spec/keys :req [::bin ::function]))

(spec/def ::validate-columns (fn [m]
                               (let [columns (::columns m)]
                                 (->> (dissoc m ::columns ::pk)
                                      (every? (fn [[k v]]
                                                (and (some #(= k %) columns)
                                                     (spec/valid? ::column v))))))))

(spec/def ::relation (spec/and (spec/keys :req [::columns])
                               ::validate-columns))
(spec/def ::root #(every? (fn [[k v]]
                           (and (keyword? k)
                                (s/valid? ::relation v)))
                          %))


