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


; TODO:
;   (deftest test-hooks)
;   (deftest test-with-progress)
;   (deftest test-with-progress-handler)
;   (deftest test-set-progress-handler)
;   (deftest test-with-progress-bar)
;   (deftest test-set-progress-bar)
;   (deftest test-config-progress-bar)
