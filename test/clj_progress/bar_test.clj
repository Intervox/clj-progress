(ns clj-progress.bar-test
  (:require [clojure.string :as s])
  (:use clj-progress.bar
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
        res)
    ":header"   [ "foo" 42  6   2   7 ] "foo"
    "[:bar]"    [ "foo" 42  6   2   7 ] "[==>   ]"
    ":wheel"    [ "foo" 42  6   2   7 ] "|"
    ":done"     [ "foo" 42  6   2   7 ] "2"
    ":total"    [ "foo" 42  6   2   7 ] "6"
    ":elapsed"  [ "foo" 42  6   2   7 ] "42"
    ":eta"      [ "foo" 42  6   2   7 ] "84"
    ":percent"  [ "foo" 42  6   2   7 ] "33%"
    ":header"   [ "bar" 99  10  5   2 ] "bar"
    "[:bar]"    [ "bar" 99  10  5   2 ] "[===>  ]"
    ":wheel"    [ "bar" 99  10  5   2 ] "\\"
    ":done"     [ "bar" 99  10  5   2 ] "5"
    ":total"    [ "bar" 99  10  5   2 ] "10"
    ":elapsed"  [ "bar" 99  10  5   2 ] "99"
    ":eta"      [ "bar" 99  10  5   2 ] "99"
    ":percent"  [ "bar" 99  10  5   2 ] "50%"))
