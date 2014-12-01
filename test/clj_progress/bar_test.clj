(ns clj-progress.bar-test
  (:require [clojure.string :as s])
  (:use (clj-progress core bar)
        clojure.test))

(defn get-state
  [[header elapsed ttl done ticks]]
  (let [now   (. System (nanoTime))
        start (- now (* elapsed 1000000000))]
    (zipmap [:header :start :ttl :done :ticks ]
            [ header  start  ttl  done  ticks ])))

(deftest test-progress-bar
  (are [fmt args res]
    (=  (let [{:keys [tick]} (progress-bar fmt :width 6)]
          (-> args get-state tick with-out-str s/trim))
        (let [{:keys [done]} (progress-bar fmt :width 6)]
          (-> args get-state done with-out-str s/trim))
        res)
    ":header"   [ "foo" 42  6   2   7 ] "foo"
    ":done"     [ "foo" 42  6   2   7 ] "2"
    ":total"    [ "foo" 42  6   2   7 ] "6"
    ":elapsed"  [ "foo" 42  6   2   7 ] "42"
    ":percent"  [ "foo" 42  6   2   7 ] "33%"

    ":header"   [ "foo" 42  0   2   7 ] "foo"
    ":done"     [ "foo" 42  0   2   7 ] "2"
    ":total"    [ "foo" 42  0   2   7 ] "?"
    ":elapsed"  [ "foo" 42  0   2   7 ] "42"
    ":percent"  [ "foo" 42  0   2   7 ] "?%"

    ":header"   [ "bar" 99  10  5   2 ] "bar"
    ":done"     [ "bar" 99  10  5   2 ] "5"
    ":total"    [ "bar" 99  10  5   2 ] "10"
    ":elapsed"  [ "bar" 99  10  5   2 ] "99"
    ":percent"  [ "bar" 99  10  5   2 ] "50%"

    ":header"   [ "bar" 99  -1  5   2 ] "bar"
    ":done"     [ "bar" 99  -1  5   2 ] "5"
    ":total"    [ "bar" 99  -1  5   2 ] "?"
    ":elapsed"  [ "bar" 99  -1  5   2 ] "99"
    ":percent"  [ "bar" 99  -1  5   2 ] "?%" )
  (are [fmt args res]
    (=  (let [{:keys [tick]} (progress-bar fmt :width 6)]
          (-> args get-state tick with-out-str s/trim))
        res)
    "[:bar]"    [ "foo" 42  6   2   7 ] "[==>   ]"
    ":wheel"    [ "foo" 42  6   2   7 ] "|"
    ":eta"      [ "foo" 42  6   2   7 ] "84"

    "[:bar]"    [ "foo" 42  0   2   7 ] "[ ===  ]"
    ":wheel"    [ "foo" 42  0   2   7 ] "|"
    ":eta"      [ "foo" 42  0   2   7 ] "?"

    "[:bar]"    [ "bar" 99  10  5   2 ] "[===>  ]"
    ":wheel"    [ "bar" 99  10  5   2 ] "\\"
    ":eta"      [ "bar" 99  10  5   2 ] "99"

    "[:bar]"    [ "bar" 99  -1  5   2 ] "[  === ]"
    ":wheel"    [ "bar" 99  -1  5   2 ] "\\"
    ":eta"      [ "bar" 99  -1  5   2 ] "?" )
  (are [fmt args res]
    (=  (let [{:keys [done]} (progress-bar fmt :width 6)]
          (-> args get-state done with-out-str s/trim))
        res)
    "[:bar]"    [ "foo" 42  6   2   7 ] "[======]"
    ":wheel"    [ "foo" 42  6   2   7 ] "+"
    ":eta"      [ "foo" 42  6   2   7 ] "0"

    "[:bar]"    [ "foo" 42  0   2   7 ] "[======]"
    ":wheel"    [ "foo" 42  0   2   7 ] "+"
    ":eta"      [ "foo" 42  0   2   7 ] "0" )
  (let [fmt   "> [:bar] :percent :done/:total :etas"
        args  [ "foo" 42 6 2 7 ]]
    (let [{:keys [tick done]} (progress-bar fmt :width 6)]
      (is (=  (-> args get-state tick with-out-str s/trim)
              "> [==>   ] 33% 2/6 84s"))
      (is (=  (-> args get-state done with-out-str s/trim)
              "> [======] 33% 2/6 0s"))))
  (let [fmt   ":header [:wheel] :percent :elapseds"
        args  [ "bar" 99 -1 5 2 ]]
    (let [{:keys [tick done]} (progress-bar fmt :width 6)]
      (is (=  (-> args get-state tick with-out-str s/trim)
              "bar [\\] ?% 99s"))
      (is (=  (-> args get-state done with-out-str s/trim)
              "bar [+] ?% 99s")))))

(defn re-count
  [re s]
  (-> (re-pattern re)
      (re-seq s)
      count))

(deftest test-concurrent-progress
  (let [fmt "Lorem ipsum"
        n   5000
        s   (with-out-str
              (with-progress-bar fmt
                (init n)
                (dorun (pmap tick (range n)))
                (done)))]
    (is (<= n (re-count (str "\r" fmt) s)))
    (is (=  0 (re-count "\r\r" s)))))
