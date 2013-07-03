(ns clj-progress.bar
  (:require [clojure.string :as string]))

(def ^:dynamic *progress-bar-options*
  { :width      50
    :complete   \=
    :incomplete \space
    :current    \> })

(defn- get-bar [percent {:keys [width complete incomplete current]}]
  {:pre [(every? char? [complete incomplete current])]}
  (let [bar (new StringBuilder)]
    (doseq [i (range width)]
      (cond (< i (int (/ percent 2))) (.append bar complete)
            (= i (int (/ percent 2))) (.append bar current)
            :else                     (.append bar incomplete)))
    (.toString bar)))

(defn- sreplace [s k v]
  (string/replace s (str k) (str v)))

(defn- update-progress-bar [fmt options {:keys [header start ttl done]}]
  (let [percent (-> done (/ ttl) (* 100) int)
        bar     (get-bar percent (merge *progress-bar-options* options))
        now     (. System (nanoTime))
        elapsed (-> now (- start) (/ 1000000000))
        eta     (-> ttl (/ done) (- 1) (* elapsed))]
    (print "\r")
    (-> fmt
        (sreplace :header header)
        (sreplace :bar bar)
        (sreplace :done done)
        (sreplace :total ttl)
        (sreplace :elapsed (long elapsed))
        (sreplace :eta (long eta))
        (sreplace :percent (str percent "%"))
        (str "     ")
        print)
    (flush)))

(defn progress-bar [fmt & {:as options}]
  { :tick (partial update-progress-bar fmt options)
    :done (fn [_] (println))})
