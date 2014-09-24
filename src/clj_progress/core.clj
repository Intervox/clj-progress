(ns clj-progress.core
  (:use clj-progress.bar))

(def ^:dynamic *progress-state* (atom {}))

(def ^:dynamic *progress-handler*
  (progress-bar ":header [:bar] :percent :done/:total"))

(defn- handle [action]
  (if-let [f (get *progress-handler* action)]
    (f @*progress-state*)))

(defn- init* [header ttl & [obj]]
  {:pre [(number? ttl)]}
  (swap! *progress-state* assoc
    :start  (. System (nanoTime))
    :ttl    ttl
    :done   0
    :header header)
  (handle :init)
  obj)

(defn init
  ([data]
    (init "" data))
  ([header data]
    (init*  header
            (if (coll? data)
                (count data)
                data)
            data))
  ([header ttl obj & args]
    (init* header ttl obj)))

(defn tick [& [obj]]
  (swap! *progress-state* update-in [:done] inc)
  (handle :tick)
  obj)

(defn tick-by [n & [obj]]
  (swap! *progress-state* update-in [:done] + n)
  (handle :tick)
  obj)

(defn tick-to [x & [obj]]
  {:pre  [(number? x)]}
  (swap! *progress-state* assoc-in [:done] x)
  (handle :tick)
  obj)

(defn done [& [obj]]
  (handle :done)
  (reset! *progress-state* {})
  obj)

(defmacro with-progress [& body]
  "Executes body incapsulating its progress"
  `(binding [*progress-state* (atom {})]
     ~@body))

(defmacro with-progress-handler [handler & body]
  `(with-progress
    (binding [*progress-handler* ~handler]
      ~@body)))

(defn set-progress-handler! [handler]
  (alter-var-root #'*progress-handler*
                  (constantly handler)))

(defmacro with-progress-bar [fmt & body]
  `(with-progress-handler (progress-bar ~fmt)
    ~@body))

(defn set-progress-bar! [fmt]
  (set-progress-handler! (progress-bar fmt)))

(defn config-progress-bar! [& {:as options}]
  (alter-var-root #'*progress-bar-options* merge options))
