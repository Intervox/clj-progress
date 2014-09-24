(ns clj-progress.core-test
  (:use clojure.test
        clj-progress.core))


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
  (are [h ticks n args]
    (let [c (atom 0)]
      (binding [*progress-handler*  {:tick (fn [_] (swap! c inc))}
                *progress-state*    (atom {})]
        (init h n)
        (dotimes [_ ticks]
          (apply tick args))
        (let [{:keys [ttl done header]} @*progress-state*]
          (and  (= ttl n)
                (= done @c ticks)
                (= header h)))))
    "foo" 1 5 [3]
    "bar" 2 9 [0]
    "baz" 7 7 [ ]))


(deftest test-tick-by
  (are [h ticks n args res]
    (binding [*progress-handler*  {}
              *progress-state*    (atom {})]
      (init h n)
      (doseq [by ticks]
        (apply tick-by by args))
      (let [{:keys [ttl done header]} @*progress-state*]
        (and  (= ttl n)
              (= done res)
              (= header h))))
    "foo" [ 1         ] 5 [3]  1
    "bar" [ 5 -7      ] 9 [0] -2
    "baz" [ 0  0  0   ] 7 [ ]  0
    "foo" [ 1 -1  1 -1] 5 [3]  0
    "bar" [ 1  2  3  4] 9 [0] 10
    "baz" [-1 -1 -1 -1] 7 [ ] -4 ))


(deftest test-tick-to
  (are [h ticks n args res]
    (binding [*progress-handler*  {}
              *progress-state*    (atom {})]
      (init h n)
      (doseq [to ticks]
        (apply tick-to to args))
      (let [{:keys [ttl done header]} @*progress-state*]
        (and  (= ttl n)
              (= done res)
              (= header h))))
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
      (init 10)
      (check :init @state)
      (tick)
      (check :tick @state)
      (tick-by 2)
      (check :tick @state)
      (tick-to 9)
      (check :tick @state)
      (let [-state @state]
        (done)
        (check :done -state)))))

; TODO:
;   (deftest test-with-progress)
;   (deftest test-with-progress-handler)
;   (deftest test-set-progress-handler)
;   (deftest test-with-progress-bar)
;   (deftest test-set-progress-bar)
;   (deftest test-config-progress-bar)
