(ns aerospike-migration.spec
  (:require [clojure.spec.alpha :as spec]
            [clojure.spec.alpha :as s]))

(spec/def ::columns sequential?)

(spec/def ::relation (s/keys :req [::columns]))
