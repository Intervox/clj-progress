(ns clj-progress.bar
  (:require [clojure.string :as string]))

(def ^:dynamic *progress-bar-options*
  { :width      50
    :complete   \=
    :incomplete \space
    :current    \> })

(defn- get-bar
  [percent {:keys [width complete incomplete current]}]
  {:pre [(every? char? [complete incomplete current])]}
  (let [bar (new StringBuilder)
        dam (-> percent (* width) (/ 100) int)]
    (doseq [i (range width)]
      (cond (< i dam) (.append bar complete)
            (= i dam) (.append bar current)
            :else     (.append bar incomplete)))
    (.toString bar)))

(defn- get-indeterminable-bar
  [ticks {:keys [width complete incomplete current]}]
  {:pre [(every? char? [complete incomplete current])]}
  (let [bar (new StringBuilder)]
    (doseq [i (range width)]
      (if (-> i (- ticks) (mod width) (< 3))
          (.append bar complete)
          (.append bar incomplete)))
    (.toString bar)))

(defn- sreplace
  [s & {:as pairs}]
  (reduce (fn [s [k v]]
            (string/replace s (str k) (str v)))
          s
          pairs))

(defn- calc-eta
  [ttl done elapsed]
  (-> ttl (/ done) (- 1) (* elapsed) long))

(defn- update-progress-bar
  [fmt options done? {:keys [header start update ttl done ticks]}]
  (let [ttl?    (pos? ttl)
        percent (if ttl? (-> done (/ ttl) (* 100)))
        opts    (merge *progress-bar-options* options)
        elapsed (-> update (- start) (/ 1000000000))]
    (print
      (sreplace (str "\r" fmt "     ")
        :header   header
        :bar      (cond
                    done? (get-bar 100 opts)
                    ttl?  (get-bar percent opts)
                    :else (get-indeterminable-bar ticks opts))
        :wheel    (if done?
                      "+"
                      (get  ["/" "-" "\\" "|"]
                            (mod ticks 4)))
        :done     done
        :total    (if ttl? ttl "?")
        :elapsed  (long elapsed)
        :eta      (cond
                    done? 0
                    ttl?  (calc-eta ttl done elapsed)
                    :else "?")
        :percent  (str (if ttl? (int percent) "?") "%")))
    (if done? (println))
    (flush)))

(defn progress-bar
  [fmt & {:as options}]
  { :tick (partial update-progress-bar fmt options false)
    :done (partial update-progress-bar fmt options true )})
