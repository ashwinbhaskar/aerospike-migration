(ns aerospike-migration.core
  (:require [mount.core :as mount]
            [aerospike-migration.aerospike :as aero]
            [aerospike-migration.postgres :as p]
            [cli-matic.core :as cli]
            [aerospike-migration.relation :as r]
            [aerospike-migration.util :as u]
            [aerospike-migration.spec :as s]
            [next.jdbc :as j])
  (:import (lockfix LockFix))
  (:gen-class))

(defmacro locking*                                          ;; patched version of clojure.core/locking to workaround GraalVM unbalanced monitor issue
  "Executes exprs in an implicit do, while holding the monitor of x.
  Will release the monitor of x in all circumstances."
  {:added "1.0"}
  [x & body]
  `(let [lockee# ~x]
     (LockFix/lock lockee# (^{:once true} fn* [] ~@body))))

(defn dynaload ;; patched version of clojure.spec.gen.alpha/dynaload to use patched locking macro
  [s]
  (let [ns (namespace s)]
    (assert ns)
    (locking* #'clojure.spec.gen.alpha/dynalock
              (require (symbol ns)))
    (let [v (resolve s)]
      (if v
        @v
        (throw (RuntimeException. (str "Var " s " is not on the classpath")))))))

(alter-var-root #'clojure.spec.gen.alpha/dynaload (constantly dynaload))

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

(defn- migrate
  [{:keys [edn-filepath hosts namespace db-host db-port db-name db-user db-pass]}]
  (start-aerospike-client hosts namespace)
  (start-postgres-datasource db-host db-port db-name db-user db-pass)
  (r/migrate (u/load-edn edn-filepath)))

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
   :subcommands [{:command     "migrate" :short "mrs"
                  :description ["Migrate table from relational db to aerospike"]
                  :opts        [{:option "edn-filepath" :short "fp" :type :string :default :present :as "An edn file containing a mapping of the columns in relational db to bins in aerospike"
                                 :spec   ::s/edn}]
                  :runs        migrate}]})

(defn -main
  [& args]
  (cli/run-cmd args CONFIGURATION))

(comment
  (start-aerospike-client "localhost" "test")
  (start-postgres-datasource "127.0.0.1" "5432" "test" "postgres" "")
  (j/execute! aerospike-migration.postgres/ds ["INSERT INTO users(first_name, last_name, phone, dob) VALUES(?,?,?,?)"]))
