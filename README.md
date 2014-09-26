clj-progress
=======

Flexible clojure progress bar, inspired by [node-progress](https://github.com/visionmedia/node-progress).

### Main functionality

 * Ascii progress bar out of the box.
 * Custom progress handlers support.

## Installation

You can install `clj-progress` using [clojars repository](https://clojars.org/intervox/clj-progress).

With Leiningen:

```Clojure
[intervox/clj-progress "0.1.3"]
```

With Gradle:

```
compile "intervox:clj-progress:0.1.3"
```

With Maven:

```xml
<dependency>
  <groupId>intervox</groupId>
  <artifactId>clj-progress</artifactId>
  <version>0.1.3</version>
</dependency>
```


## Usage

Using `clj-progress` is really simple.

There are three main methods defining the progress:

 * `init`
 * `tick`
 * `done`

`init` method takes the name of the progress as its first optional argument and the total number of ticks as the second one. If the second argument is a collection it count its element.

When the third argument is provided, `init` returns it.

`tick` and `done` takes no arguments, returning the first argument if any provided.

### Examples

```Clojure
(use 'clj-progress.core)

(defn progress []
  (init 50)
  (reduce + (map  #(do (tick) (Thread/sleep 200) %)
                  (range 50)))
  (done))
```

More clojureish way to use progress:

```Clojure
(use 'clj-progress.core)

(defn process-item [item]
  (Thread/sleep 200)
  item)

(defn greedy []
  (->>  (range 50)
        (init "Processing")
        (map (comp tick process-item))
        (reduce +)
        done))
```

Processing lazy sequences with progress:

```Clojure
(defn lazy []
  (->>  (iterate inc 0)
        (init "Processing" 50)
        (map (comp tick process-item))
        (take 50)
        (reduce +)
        done))
```

## Other ticking methods

`clj-progress` also provides two extra tick methods:

 * `(tick-by n)` - will tick by an amount of `n`
 * `(tick-to x)` - will set current progress value to `x`

The first argument for `tick-by` and `tick-to` is mandatory.

Both `tick-by` and `tick-to` will return their second argument if any provided.

### Examples

```Clojure
(use 'clj-progress.core)
(use 'clojure.java.io)

(defn copy-file [input-path output-path]
  (init (-> (file input-path) .length))
  (with-open [input  (input-stream  input-path )
              output (output-stream output-path)]
    (let [buffer (make-array Byte/TYPE 1024)]
      (loop []
        (let [size (.read input buffer)]
          (when (pos? size)
            (.write output buffer 0 size)
            (tick-by size)
            (recur))))))
  (done))
```

## Customizing progress bar

You can customize progress bar using `set-progress-bar!` and `config-progress-bar!` methods.

`set-progress-bar!` takes the format string as its single argument. You can use the following set of tokens to create your own proress bar:

 * `:header` name of the progress
 * `:bar` the progress bar itself
 * `:done` current tick number
 * `:total` total number of ticks
 * `:elapsed` elapsed time in seconds
 * `:eta` estimated completion time in seconds
 * `:percent` completion percentage

By default it set to `:header [:bar] :percent :done/:total`.

`config-progress-bar!` allows you to customize the progress bar itself:

 * `:width` width of the progress bar (default `50`)
 * `:complete` completion character (default `\=`)
 * `:incomplete` incomplete character (default `\space`)
 * `:current` current tick character (default `\>`)

```Clojure
(set-progress-bar! "  downloading [:bar] :percent :etas")

(config-progress-bar!
  :complete   \#
  :current    \#
  :incomplete \-)
```

## Using custom progress handlers

`clj-proggress` allows you to use your own progress handler by defining `:init`, `:tick` and `:done` hooks:

```Clojure
(use 'clj-progress.core)

(defn process-item [item]
  (-> (rand)
      (* 1000)
      int
      Thread/sleep)
  item)

(defn process-all []
  (init 50)
  (reduce + (map  (comp tick process-item)
                  (range 50)))
  (done))

(defn update-atom [state data]
  (swap! state merge data))

(defn atom-handler [a]
  { :init (partial update-atom a)
    :tick (partial update-atom a)
    :done (fn [_] (swap! a assoc :ready true))})

(defn atomic []
  (let [state (atom {:ready false})]
    (with-progress-handler (atom-handler state)
      (future (process-all)))
    state))

(defn multi []
  (let [atoms (repeatedly 5 atomic)]
    (while (some (comp not :ready deref) atoms)
      (dotimes [i 5]
        (let [{:keys [ttl done]} (deref (nth atoms i))]
          (println i ":" done "/" ttl)))
      (println "==========")
      (Thread/sleep 1000))
    (println "All done!")))
```

## License

Copyright © 2013-2014 Leonid Beschastny

Distributed under the Eclipse Public License, the same as Clojure.
