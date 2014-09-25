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
  [s k v]
  (string/replace s (str k) (str v)))

(defn- calc-eta
  [ttl done elapsed]
  (-> ttl (/ done) (- 1) (* elapsed) long))

(defn- update-progress-bar
  [fmt options done? {:keys [header start ttl done ticks]}]
  (let [ttl?    (pos? ttl)
        percent (if ttl?
                    (-> done (/ ttl) (* 100) int)
                    "?")
        opts    (merge *progress-bar-options* options)
        bar     (if ttl?
                    (get-bar percent opts)
                    (get-indeterminable-bar ticks opts))
        wheel   (if done?
                    "+"
                    (get  ["-" "\\" "|" "/"]
                          (mod ticks 4)))
        now     (. System (nanoTime))
        elapsed (-> now (- start) (/ 1000000000))
        eta     (cond
                  done? 0
                  ttl?  (calc-eta ttl done elapsed)
                  :else "?")]
    (print "\r")
    (-> fmt
        (sreplace :header header)
        (sreplace :bar bar)
        (sreplace :wheel wheel)
        (sreplace :done done)
        (sreplace :total (if ttl? ttl "?"))
        (sreplace :elapsed (long elapsed))
        (sreplace :eta eta)
        (sreplace :percent (str percent "%"))
        (str "     ")
        print)
    (if done? (println))
    (flush)))

(defn progress-bar
  [fmt & {:as options}]
  { :tick (partial update-progress-bar fmt options false)
    :done (partial update-progress-bar fmt options true )})
