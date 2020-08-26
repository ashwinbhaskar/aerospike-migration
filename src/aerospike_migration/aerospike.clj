(ns aerospike-migration.aerospike
  (:require [aerospike-clj.client :as aero]
            [mount.core :as m]
            [clojure.string :as str]))

(defn- hosts [comma-separated-hosts]
  (->> (str/split comma-separated-hosts #",")
       (map str/trim)))

(m/defstate client
  :start (aero/init-simple-aerospike-client
           (hosts (System/getProperty "hosts"))
           (System/getProperty "namespace")
           {:enable-logging true})
  :stop (.close client))


