(ns aerospike-migration.postgres
  (:require [mount.core :as m]
            [next.jdbc :as jdbc]
            [clojure.tools.logging :as log]))

(defn- jdbc-url
  [host port name username password]
  (format
    "jdbc:postgresql://%s:%s/%s?user=%s&password=%s"
    host
    port
    name
    username
    password))

(m/defstate ds
  :start (jdbc/get-datasource {:jdbcUrl (jdbc-url
                                         (System/getProperty "db_host")
                                         (System/getProperty "db_port")
                                         (System/getProperty "db_name")
                                         (System/getProperty "db_username")
                                         (System/getProperty "db_password"))})
  :stop  nil)
