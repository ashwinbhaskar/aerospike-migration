(ns aerospike-migration.relation
  (:require [aerospike-migration.util :as u]
            [aerospike-migration.transformer :as t]
            [next.jdbc :as j]
            [aerospike-migration.postgres :refer [ds]]
            [aerospike-migration.spec :as s]
            [clojure.string :as str]
            [camel-snake-kebab.core :as csk]
            [aerospike-clj.client :as aero]
            [aerospike-migration.aerospike :refer [client]])
  (:import (java.util.function BiFunction)
           (java.util.concurrent CompletableFuture)))

(defn- prepare-query
  [columns relation]
  (format "SELECT %s FROM %s" (str/join "," columns) relation))

(defn- column-name->bin-name
  [mapping relation column-name]
  (-> mapping
      relation
      column-name
      ::s/bin))

(defn- column-value->bin-value
  [mapping relation column-name value]
  (let [transformer      (-> mapping
                             relation
                             column-name
                             ::s/function
                             t/functions)
        transformer-args (-> mapping
                             relation
                             column-name
                             ::s/function-args)]
    (apply transformer value transformer-args)))

(defn- column->bin
  [mapping relation column-name value]
  {(column-name->bin-name mapping relation column-name)
   (column-value->bin-value mapping relation column-name value)})

(defn- row->bin
  [row mapping relation]
  (reduce (fn [acc [k v]]
            (into acc (column->bin mapping relation k v)))
          {}
          row))

(defn- sanitize-row
  [row]
  (let [r-fn (fn [acc [k v]]
               (assoc acc k (u/pg-object->map v)))]
    (reduce r-fn {} row)))

(defn- index
  [row pk-info]
  (let [delimiter (::s/pk-info.delimiter pk-info)
        prepend   (u/if-not-blank (::s/pk-info.prepend pk-info)
                                  #(str % delimiter))
        append    (u/if-not-blank (::s/pk-info.append pk-info)
                                  #(str delimiter %))
        mapper    (fn [k]
                    (let [transformer (t/functions (get-in pk-info [k ::s/function]))]
                      (-> (k row)
                          transformer)))
        pks       (->> (map mapper (::s/pk-info.primary-keys pk-info))
                       (str/join delimiter))]
    (str prepend
         pks
         append)))

(defn- bi-function ^BiFunction
  [debug-logger]
  (reify BiFunction
    (apply [_ r exception]
      (if (not (nil? exception))
        (.write debug-logger (format "Exception: %s exception" exception))))))

(defn- dump-into-aerospike
  [rows debug-logger]
  (let [[indexes payloads sets] (reduce
                                  (fn [[index-acc payload-acc set-acc] {:keys [index data set-name]}]
                                    [(conj index-acc index)
                                     (conj payload-acc data)
                                     (conj set-acc set-name)])
                                  [[] [] []]
                                  rows)
        expirations (repeat (count rows) -1)
        future (aero/put-multiple client indexes sets payloads expirations)]
    (-> (.handle ^CompletableFuture future (bi-function debug-logger))
        .join)))

(defn- migrate-relation
  [mapping [relation v]]
  (let [columns          (set (::s/columns v))
        columns-in-query (->> (map name columns)
                              (map #(csk/->snake_case %)))
        query            (prepare-query columns-in-query (-> (name relation)
                                                             csk/->snake_case))
        set-name         (::s/set-name v)
        rs               (j/execute! ds [query])
        batch-size       (::s/batch-size v)
        slices           (if (contains? v ::s/row-count)
                           (u/slice-lazy rs batch-size (::s/row-count v))
                           (u/slice rs batch-size))]
    (println (str "Starting migration of " (name relation)))
    (with-open [debug-logger (clojure.java.io/writer (format "%s-debug-logger.txt" (name relation)) :append true)]
      (->> slices
           (map #(map u/kebab-caseize %))
           (map #(map sanitize-row %))
           (map (fn [rows]
                  (map (fn [row]
                         (let [index-keys (-> (select-keys row (get-in v [::s/pk-info ::s/pk-info.primary-keys]))
                                              keys)
                               data       (row->bin (apply #(dissoc row %) index-keys) mapping relation)
                               index      (index row (::s/pk-info v))]
                           {:data     data
                            :index    index
                            :set-name set-name}))
                       rows)))
           (run! #(dump-into-aerospike % debug-logger))))))

(defn migrate
  [m]
  (run! #(migrate-relation m %) m))
