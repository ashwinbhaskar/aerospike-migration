(ns segmentation-migration.util)

(defn slice [v n]
  (let [length (count v)]
    (if (>= n length)
      [(into [] v)])
    (let [m (mod length n)]
      (conj
        (mapv #(into [] %) (partition n n v))
        (into [] (drop (- length m) v))))))
