(ns aerospike-migration.relation
  (:require [aerospike-migration.util :as u]
            [next.jdbc :as j]
            [aerospike-migration.postgres :refer [ds]]
            [aerospike-migration.spec :as s]
            [clojure.string :as str]
            [camel-snake-kebab.core :as csk]
            [aerospike-clj.client :as aero]
            [aerospike-migration.aerospike :refer [client]]))

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
  (let [transformer (-> mapping
                        relation
                        column-name
                        ::s/function
                        u/functions)]
    (transformer value)))

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
                    (let [transformer (u/functions (get-in pk-info [k ::s/function]))]
                      (-> (k row)
                          transformer)))
        pks       (->> (map mapper (::s/pk-info.primary-keys pk-info))
                       (str/join delimiter))]
    (str prepend
         pks
         append)))

(defn- migrate-relation
  [mapping [relation v]]
  (let [columns          (set (::s/columns v))
        columns-in-query (->> (map name columns)
                              (map #(csk/->snake_case %)))
        query            (prepare-query columns-in-query (name relation))
        set-name         (::s/set-name v)]
    (->> (j/execute! ds [query])
         (map u/kebab-caseize)
         (map sanitize-row)
         (map (fn [row]
                (let [index-keys (-> (select-keys row (get-in v [::s/pk-info ::s/pk-info.primary-keys]))
                                     keys)
                      data       (row->bin (apply #(dissoc row %) index-keys) mapping relation)
                      index      (index row (::s/pk-info v))]
                  {:data  data
                   :index index})))
         (run! (fn [{:keys [data index]}]
                 (aero/put client index set-name data -1))))))

(defn migrate
  [m]
  (run! #(migrate-relation m %) m))
