(defproject segmentation-migration "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [aerospike-clj "1.0.0"]
                 [mount "0.1.16"]
                 [cli-matic "0.4.3"]]
  :main segmentation-migration.core
  :repl-options {:init-ns segmentation-migration.core})
