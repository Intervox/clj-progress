(ns clj-progress.core
  (:use clj-progress.bar))

(def ^:dynamic *progress-state* (atom {}))

(def ^:dynamic *progress-handler*
  (progress-bar ":header [:bar] :percent :done/:total"))

(def ^:dynamic *throttle* 20)

(defn- handle [action]
  (if-let [f (get *progress-handler* action)]
    (f @*progress-state*)))

(defn- init* [header ttl & [obj]]
  {:pre [(number? ttl)]}
  (let [now (System/nanoTime)]
    (swap! *progress-state* assoc
      :start  now
      :update now
      :ttl    ttl
      :done   0
      :ticks  0
      :header header))
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

(defn re-init
  [ttl]
  (swap! *progress-state* update-in [:ttl] ttl)
  (handle :tick))

(defn- tick* [obj]
  (let [wait  (if (pos? *throttle*)
                  (* *throttle* 1000000)
                  -1)
        prev  (get @*progress-state* :update)
        now   (System/nanoTime)]
    (when (> (- now prev) wait)
      (doto *progress-state*
        (swap! update-in  [:ticks ] inc)
        (swap! assoc-in   [:update] now))
      (handle :tick)))
  obj)

(defn tick [& [obj]]
  (swap! *progress-state* update-in [:done] inc)
  (tick* obj))

(defn tick-by [n & [obj]]
  (swap! *progress-state* update-in [:done] + n)
  (tick* obj))

(defn tick-to [x & [obj]]
  {:pre  [(number? x)]}
  (swap! *progress-state* assoc-in [:done] x)
  (tick* obj))

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

(defmacro with-throttle [wait & body]
  `(binding [*throttle* ~wait]
     ~@body))

(defn set-throttle! [wait]
  (alter-var-root #'*throttle*
                  (constantly wait)))
