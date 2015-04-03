# clj-fst

This Clojure FST implementation is a wrapper above the Lucene FST package which is part of Lucene core.

Finite state transducers are finite state machines with two tapes: an input and an output tape. The automaton map an input string to an output. The output can be another string or an integer.

The FST produced by this application are implemented as a bytes array which makes FST really effective indexes in terms of speed and memory consumption. In fact, a 10 millions terms index will takes roughtly 256 MB of memory (depending of the string composition of the input strings, and if the outputs are integers or strings).

`clj-fst` is an lightning-fast and memory effective way to figure out if something belong to a really huge set of things, or to get the output of an input. This is really simple but has profound implications.

## Installation

### Using Leiningen

You can easily install `clj-fst` using Leiningen. The only thing you have to do is to add Add `[clj-fst "0.1.0"]` as a dependency to your `project.clj`.

Then make sure that you downloaded this dependency by running the `lein deps` command.

## Documentation

[The complete `clj-fst` documentation is available here.](http://structureddynamics.github.io/clj-fst/)

## Usage

Here is how you can create, populate and save a FST:

```clojure
;; The first thing to do is to create the Builder
(def builder (create-builder! :type :int))

;; This small `sorted-map` defines the things
;; to add to the FST
(def values (into (sorted-map) {"cat" 1
                                "dog" 2
                                "mice" 3}))

;; Populate the FST using that `sorted-map`
(doseq [[input output] values]
  (add! builder {input output}))

;; Save a FST on the file system
(save! "resources/fst.srz" fst)
```

Additionally you can load a previously saved FST:

```clojure
;; Load a FST from the file system
(load! "resources/fst.srz)
```

You can query a FST to get the output related to an input:

```clojure
;; Query the FST
(get-output "cat" fst)
```

You can iterate over a FST using FST enumerations:

```clojure
;; Create the FST enumeration
(def enum (create-enum! fst))

;; Get the first item in the FST
(next! enum)

;; Get the current FST item pointed by the enumerator
(current! enum)

;; Search for different input terms
(get-ceil-term! "cat" enum)

(get-floor-term! "cat" enum)

(get-exact-term! "cat" enum)
```
