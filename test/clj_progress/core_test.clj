(ns clj-progress.core-test
  (:use (clj-progress core bar)
        clojure.test))


(deftest test-returned-value
  (are [args]
    (binding [*progress-handler* {}]
      (let [res (last args)]
        (=  res
            (apply init args)
            (tick res)
            (tick-by 2 res)
            (tick-to 5 res)
            (done res))))
    ["" 1 2       ]
    ["" 1 ""      ]
    ["" 1 [1]     ]
    ["" 1 {}      ]
    [""   1       ]
    [""   [2]     ]
    [""   (list 2)]
    [""   #{2}    ]
    [""   {:q 2}  ]
    [     1       ]
    [     [2]     ]
    [     (list 2)]
    [     #{2}    ]
    [     {:q 2}  ]))


(deftest test-init
  (are [args]
    (binding [*progress-state* (atom {})]
      (apply init args)
      (let [{:keys [ttl done header]} @*progress-state*]
        (and  (= 1  ttl)
              (= 0  done)
              (= "" header))))
    ["" 1 2       ]
    ["" 1 ""      ]
    ["" 1 [1]     ]
    ["" 1 {}      ]
    [""   1       ]
    [""   [2]     ]
    [""   (list 2)]
    [""   #{2}    ]
    [""   {:q 2}  ]
    [     1       ]
    [     [2]     ]
    [     (list 2)]
    [     #{2}    ]
    [     {:q 2}  ])
  (are [args res]
    (binding [*progress-state* (atom {})]
      (apply init args)
      (let [{:keys [ttl done header]} @*progress-state*]
        (and  (= ttl res)
              (= done 0)
              (= header (first args)))))
    ["foo" 1 2       ]  1
    ["bar" 9 ""      ]  9
    ["baz" 7 [1 2 3] ]  7
    ["foo" 3 {}      ]  3
    ["bar"   9       ]  9
    ["baz"   [3 1]   ]  2
    ["foo"   (list 2)]  1
    ["bar"   #{2 3 4}]  3
    ["baz"   {:q 2}  ]  1)
  (is
    (let [c (atom 0)]
      (binding [*progress-handler* {:init (fn [_] (swap! c inc))}]
        (init 123)
        (init 456)
        (= @c 2)))))


(deftest test-tick
  (are [h nticks n args]
    (let [c (atom 0)]
      (binding [*progress-handler*  {:tick (fn [_] (swap! c inc))}
                *progress-state*    (atom {})]
        (init h n)
        (with-throttle 0
          (dotimes [_ nticks]
            (apply tick args)))
        (let [{:keys [ttl done ticks header]} @*progress-state*]
          (and  (= ttl n)
                (= done @c ticks nticks)
                (= header h)))))
    "foo" 1 5 [3]
    "bar" 2 9 [0]
    "baz" 7 7 [ ]))


(deftest test-throttle
  (are [nticks throttle sleep]
    (let [c   (atom 0)
          n   (-> nticks
                  (* sleep)
                  (/ throttle)
                  int)]
      (binding [*progress-handler*  {:tick (fn [_] (swap! c inc))}
                *progress-state*    (atom {})
                *throttle*          throttle]
        (init n)
        (dotimes [_ nticks]
          (Thread/sleep sleep)
          (tick))
        (let [{:keys [ttl done ticks header]} @*progress-state*]
          (and  (= @c ticks)
                (>= nticks ticks n)))))
    100   200 20
    100   20  5
    1000  200 5 ))


(deftest test-tick-by
  (are [h bys n args res]
    (let [c (atom 0)]
      (binding [*progress-handler*  {:tick (fn [_] (swap! c inc))}
                *progress-state*    (atom {})]
        (init h n)
        (with-throttle 0
          (doseq [by bys]
            (apply tick-by by args)))
        (let [{:keys [ttl done ticks header]} @*progress-state*]
          (and  (= ttl n)
                (= done res)
                (= @c ticks)
                (= header h)))))
    "foo" [ 1         ] 5 [3]  1
    "bar" [ 5 -7      ] 9 [0] -2
    "baz" [ 0  0  0   ] 7 [ ]  0
    "foo" [ 1 -1  1 -1] 5 [3]  0
    "bar" [ 1  2  3  4] 9 [0] 10
    "baz" [-1 -1 -1 -1] 7 [ ] -4 ))


(deftest test-tick-to
  (are [h tos n args res]
    (let [c (atom 0)]
      (binding [*progress-handler*  {:tick (fn [_] (swap! c inc))}
                *progress-state*    (atom {})]
        (init h n)
        (with-throttle 0
          (doseq [to tos]
            (apply tick-to to args)))
        (let [{:keys [ttl done ticks header]} @*progress-state*]
          (and  (= ttl n)
                (= done res)
                (= @c ticks)
                (= header h)))))
    "foo" [ 1         ] 5 [3]  1
    "bar" [ 5 -7      ] 9 [0] -7
    "baz" [ 0 -1  1   ] 7 [ ]  1
    "foo" [ 1 -1  1  1] 5 [3]  1
    "bar" [ 1  2  3  4] 9 [0]  4 ))


(deftest test-done
  (are [args]
    (let [c (atom 0)]
      (binding [*progress-handler*  {:done (fn [_] (swap! c inc))}
                *progress-state*    (atom {})]
        (init "foo" 10)
        (tick)
        (apply done args)
        (and  (= @*progress-state* {})
              (= @c 1))))
    [3]
    [ ]))


(deftest test-lazy
  (let [c1 (atom 0)
        c2 (atom 0)
        c3 (atom 0)]
    (binding [*progress-handler*  { :init (fn [_] (swap! c1 inc))
                                    :tick (fn [_] (swap! c2 inc))
                                    :done (fn [_] (swap! c3 inc)) }
              *progress-state*    (atom {})]
      (with-throttle 0
        (->> (range 50)
             (init "Processing")
             (map tick)
             done
             dorun))
      (is (= @*progress-state* {}))
      (is (= @c1 1 ))
      (is (= @c2 50))
      (is (= @c3 1 )))))


(deftest test-hooks
  (let [state   (atom {})
        log     (atom [])
        hook    (fn [-name]
                  #(swap! log conj [-name %]))
        handler { :init (hook :init)
                  :tick (hook :tick)
                  :done (hook :done)}
        nops    (atom 0)
        check   (fn [expected-name expected-state]
                  (swap! nops inc)
                  (is (= @nops (count @log)))
                  (let [[-name -state] (last @log)]
                    (is (= -name  expected-name ))
                    (is (= -state expected-state))))]
    (binding [*progress-handler*  handler
              *progress-state*    state]
      (with-throttle 0
        (init 10)
        (check :init @state)
        (tick)
        (check :tick @state)
        (tick-by 2)
        (check :tick @state)
        (tick-to 9)
        (check :tick @state))
      (let [-state @state]
        (done)
        (check :done -state)))))


(deftest test-with-progress
  (let [mock  { :foo :bar }
        state (atom mock)]
    (binding [*progress-handler*  {}
              *progress-state*    state]
      (with-progress
        (is (not= *progress-state* state))
        (is (= @*progress-state* {}))
        (init 10))
      (is (= *progress-state* state))
      (is (= @state mock)))))


(deftest test-with-progress-handler
  (let [h1  { :foo :bar }
        h2  { :foo :baz }]
    (binding [*progress-handler* h1]
      (with-progress-handler h2
        (is (= *progress-handler* h2)))
      (is (= *progress-handler* h1)))))


(deftest test-set-progress-handler
  (let [h1    { :foo :bar }
        h2    { :foo :baz }
        curr  *progress-handler*]
    (binding [*progress-handler* h1]
      (set-progress-handler! h2)
      (is (= *progress-handler* h1)))
    (is (= *progress-handler* h2))
    (set-progress-handler! curr)
    (is (= *progress-handler* curr))))


(deftest test-with-progress-bar
  (let [h1  { :foo :bar }
        h2  { :foo :baz }]
    (with-redefs-fn { #'progress-bar (constantly h2) }
      #(binding [*progress-handler* h1]
        (with-progress-bar "fmt"
          (is (= *progress-handler* h2)))
        (is (= *progress-handler* h1))))))


(deftest test-set-progress-bar
  (let [h1    { :foo :bar }
        h2    { :foo :baz }
        curr  *progress-handler*]
    (with-redefs-fn { #'progress-bar (constantly h2) }
      #(binding [*progress-handler* h1]
        (set-progress-bar! "fmt")
        (is (= *progress-handler* h1))))
    (is (= *progress-handler* h2))
    (set-progress-handler! curr)))


(deftest test-config-progress-bar
  (let [opts  *progress-bar-options*]
    (binding [*progress-bar-options* {}]
      (config-progress-bar! :foo :bar :baz 42)
      (is (= *progress-bar-options* {})))
    (is (=  *progress-bar-options*
            (assoc opts :foo :bar :baz 42)))
    (alter-var-root #'*progress-bar-options*
                    (constantly opts))))


(deftest test-with-throttle
  (let [t1 666 t2 999]
    (binding [*throttle* t1]
      (with-throttle t2
        (is (= *throttle* t2)))
      (is (= *throttle* t1)))))


(deftest test-set-throttle
  (let [t1    666
        t2    999
        curr  *throttle*]
    (binding [*throttle* t1]
      (set-throttle! t2)
      (is (= *throttle* t1)))
    (is (= *throttle* t2))
    (set-throttle! curr)
    (is (= *throttle* curr))))
