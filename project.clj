(defproject aerospike-migration "0.1.0"
  :description "Migrate data from relational databases to aerospike"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [aerospike-clj "1.0.0"]
                 [mount "0.1.16"]
                 [cli-matic "0.4.3"]
                 [seancorfield/next.jdbc "1.1.582"]
                 [hikari-cp "2.13.0"]
                 [org.postgresql/postgresql "42.2.16"]
                 [camel-snake-kebab "0.4.1"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [metosin/jsonista "0.2.7"]
                 [failjure "2.0.0"]]
  :main aerospike-migration.core
  :repl-options {:init-ns aerospike-migration.core})
