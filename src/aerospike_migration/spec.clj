(ns aerospike-migration.spec
  (:require [clojure.spec.alpha :as spec]
            [aerospike-migration.util :as u]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(spec/def ::non-blank-string (spec/and string? (complement str/blank?)))

(spec/def ::keyword-seq (spec/and sequential?
                                  #(every? keyword? %)))

(spec/def ::columns ::keyword-seq)

(spec/def ::function #{:identity :timestamp->epoch-seconds :->string})

(spec/def ::bin string?)

(spec/def ::column (spec/keys :req [::bin ::function]))

(spec/def ::set-name ::non-blank-string)

(spec/def ::validate-columns (fn [m]
                               (let [columns (::columns m)]
                                 (->> (dissoc m ::columns ::pk-info)
                                      (every? (fn [[k v]]
                                                (and (some #(= k %) columns)
                                                     (spec/valid? ::column v))))))))
(spec/def ::validate-primary-keys (fn [m]
                                    (let [column?      (set (::columns m))
                                          primary-keys (get-in m [::pk-info ::pk-info.primary-keys])]
                                      (and (every? column? primary-keys)
                                           (every? (fn [k]
                                                     (->> (get-in m [::pk-info k ::function])
                                                          (spec/valid? ::function)))
                                                   primary-keys)))))

(spec/def ::pk-info.prepend string?)

(spec/def ::pk-info.append string?)

(spec/def ::pk-info.delimiter string?)

(spec/def ::pk-info.primary-keys ::keyword-seq)

(spec/def ::pk-info (spec/keys :req [::pk-info.append ::pk-info.delimiter ::pk-info.delimiter ::pk-info.primary-keys]))

(spec/def ::relation (spec/and (spec/keys :req [::columns ::pk-info ::set-name])
                               ::validate-columns
                               ::validate-primary-keys))

(spec/def ::root #(every? (fn [[k v]]
                            (and (keyword? k)
                                 (spec/valid? ::relation v)))
                          %))

(spec/def ::edn #(->> (u/load-edn %)
                      (s/valid? ::root)))


