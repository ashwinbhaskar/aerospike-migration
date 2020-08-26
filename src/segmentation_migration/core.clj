(ns segmentation-migration.core
  (:require [segmentation-migration.segments :as s]
            [mount.core :as mount]
            [clojure.string :as str]
            [segmentation-migration.aerospike :as aero]
            [cli-matic.core :as cli])
  (:gen-class))

(defn- start-aerospike-client [comma-separated-hosts namespace]
  (System/setProperty "hosts" comma-separated-hosts)
  (System/setProperty "namespace" namespace)
  (mount/start))

(defn- migrate-segments [{:keys [filepath batch-size hosts namespace]}]
  (start-aerospike-client hosts namespace)
  (s/migrate-segments filepath batch-size))

(def CONFIGURATION
  {:command     "segmentation-database-migration"
   :description "A CLI to migrate data segmentation service db to aerospike"
   :version     "0.1"

   :opts        [{:option "hosts" :short "hs" :type :string :default :present :as "The aerospike database hosts separated by comma"}
                 {:option "namespace" :short "ns" :type :string :default :present :as "Aerospike namespace"}]
   :subcommands [{:command     "migrate-segments" :short "ms"
                  :description ["Migrate segments table"]
                  :opts        [{:option "filepath" :short "fp" :type :string :default :present :as "The path of the csv file. Columns should be in the order - segment-name, created-at, deleted, unit-type"}
                                {:option "batch-size" :short "bsz" :type :int :default :present :as "The number of concurrent migrations that should be made"}]
                  :runs        migrate-segments}]})

(defn -main
  [& args]
  (cli/run-cmd args CONFIGURATION))
