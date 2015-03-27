# clj-progress [![Build Status][travis_badge]][travis] [![Dependency Status][versioneye_badge]][versioneye]

  [travis_badge]: https://travis-ci.org/Intervox/clj-progress.svg
  [versioneye_badge]: http://www.versioneye.com/clojure/intervox:clj-progress/badge.svg
  [travis]: https://travis-ci.org/Intervox/clj-progress
  [versioneye]: http://www.versioneye.com/clojure/intervox:clj-progress

Flexible clojure progress bar, inspired by [node-progress].

  [node-progress]: https://github.com/visionmedia/node-progress

### Main functionality

 * Ascii progress bar out of the box.
 * Custom progress handlers support.

## Installation

You can install `clj-progress` using [clojars repository][repo].

  [repo]: https://clojars.org/intervox/clj-progress

With Leiningen:

```Clojure
[intervox/clj-progress "0.2.0"]
```

With Gradle:

```
compile "intervox:clj-progress:0.2.0"
```

With Maven:

```xml
<dependency>
  <groupId>intervox</groupId>
  <artifactId>clj-progress</artifactId>
  <version>0.2.0</version>
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

## Hot re-initialization

Sometimes an exact number of ticks is unknown when at progress bar initialization time.
To adjust total number of ticks without reseting the whole progress bar you may use `re-init` function:

```Clojure
(re-init 60)
```

## Customizing progress bar

You can customize progress bar using `set-progress-bar!` and `config-progress-bar!` methods and `with-progress-bar` macro.

`set-progress-bar!` takes the format string as its single argument. You can use the following set of tokens to create your own progress bar:

 * `:header` name of the progress
 * `:bar` the progress bar itself
 * `:wheel` rotating progress indicator
 * `:done` current tick number
 * `:total` total number of ticks
 * `:elapsed` elapsed time in seconds
 * `:eta` estimated completion time in seconds
 * `:percent` completion percentage

By default it set to `:header [:bar] :percent :done/:total`.

`with-progress-bar` macro allows you set progress bar format string only for some part of your code, without changing global settings.

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

(with-progress-bar "[:wheel] :done/:total :header"
  (do-something))
```

## Indeterminable progress bar

To use `clj-progress` with unknown

When total number of ticks is unknown it's possible to start indeterminable progress
by initializing it with any non-positive value:

```Clojure
(init "downloading" -1)
```

or

```Clojure
(init 0)
```

When progress state is indeterminable, following `progress-bar` tokens will be replaced with `?` symbol:

 * `:total`
 * `:eta`
 * `:percent`

Indeterminable state also change `:bar` animation.

## Throttling

`clj-progress` limits the frequency of progress bar updates. Default configuration allows at most one update per every `20` milliseconds (maximum `50` updated per second).

`clj-progress` will execute `:tick` progress handler (reprint progress bar, or invoke user-defined handler) as soon as you'll call any `tick` method for the first time.
If you'll call it again any number of times during the wait period, `:tick` progress handler will not be executed, though progress status will be tracked internally.

You could change default behavior using `set-throttling!` function and `with-throttling` macro:

```Clojure
(set-throttling! wait-time-in-milliseconds)

(with-throttling wait-time-in-milliseconds
  (do-something))
```

Any non-positive value will completely disable throttling.

## Using custom progress handlers

`clj-proggress` allows you to use your own progress handler by defining `:init`, `:tick` and `:done` hooks with `set-progress-handler!` method or `with-progress-handler` macro:

```Clojure
(set-progress-handler!
  { :init init-handler
    :tick tick-handler
    :done done-handler})

(with-progress-handler my-handler
  (do-something))
```

### Example

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

## Handling progress state

`clj-progress` keeps a global state to track your progress status. Sometimes it may be useful to create a local execution state (for example, if you want to execute several tasks in parallel with custom progress handler). You could do it using `with-progress` macro:

```Clojure
(with-progress
  (do-something))
```

## [Changelog][history]

  [history]: History.md

## License

Copyright © 2013-2014 Leonid Beschastny

Distributed under the Eclipse Public License, the same as Clojure.
