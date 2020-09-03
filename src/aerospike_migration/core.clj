(ns aerospike-migration.core
  (:require [mount.core :as mount]
            [aerospike-migration.aerospike :as aero]
            [aerospike-migration.postgres :as p]
            [cli-matic.core :as cli]
            [aerospike-migration.relation :as r]
            [aerospike-migration.util :as u]
            [aerospike-migration.spec :as s])
  (:gen-class))

(defn- start-aerospike-client
  [comma-separated-hosts namespace]
  (System/setProperty "hosts" comma-separated-hosts)
  (System/setProperty "namespace" namespace)
  (mount/start #'aero/client))

(defn start-postgres-datasource
  [host port name user pass]
  (System/setProperty "db_host" host)
  (System/setProperty "db_port" (str port))
  (System/setProperty "db_name" name)
  (System/setProperty "db_username" user)
  (System/setProperty "db_password" pass)
  (mount/start #'p/ds))

(defn- migrate-relation
  [{:keys [edn-filepath relation-name batch-size hosts namespace db-host db-port db-name db-user db-pass]}]
  (start-aerospike-client hosts namespace)
  (start-postgres-datasource db-host db-port db-name db-user db-pass)
  (r/migrate relation-name edn-filepath batch-size))

(def CONFIGURATION
  {:command     "aerospike-database-migration"
   :description "A CLI to migrate data aerospike service db to aerospike"
   :version     "0.1"

   :opts        [{:option "hosts" :short "hs" :type :string :default :present :as "The aerospike database hosts separated by comma"}
                 {:option "namespace" :short "ns" :type :string :default :present :as "Aerospike namespace"}
                 {:option "db-host" :short "dbh" :type :string :default :present :as "Postgresql host"}
                 {:option "db-port" :short "dbp" :type :int :default 5432 :as "Postgresql port"}
                 {:option "db-name" :short "dbn" :type :string :default :present :as "Database name"}
                 {:option "db-user" :short "dbu" :type :string :default :present :as "Database user"}
                 {:option "db-pass" :short "dbps" :type :string :default :present :as "Database password"}]
   :subcommands [{:command     "migrate-relations" :short "mrs"
                  :description ["Migrate table from relational db to aerospike"]
                  :opts        [{:option "edn-filepath" :short "fp" :type :string :default :present :as "An edn file containing a mapping of the columns in relational db to bins in aerospike"
                                 :spec   ::s/edn}
                                {:option "batch-size" :short "bsz" :type :int :default :present :as "The number of concurrent migrations that should be made"}]
                  :runs        migrate-relation}]})

(defn -main
  [& args]
  (cli/run-cmd args CONFIGURATION))

(comment
  (start-aerospike-client "localhost" "test")
  (start-postgres-datasource "127.0.0.1" "5432" "aerospike_service" "postgres" ""))
