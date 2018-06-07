;; # FST Enumerations
;;
;; FST enumerations are used to search for the existance of inputs in the FST.
;; The first thing you have to do is to create an enumeration from a FST using
;; the `create-enum!` function. Then you have a series of utility function
;; that you can use to seek for a given input term.

(ns clj-fst.enum
  (:use [clj-fst.core]
        [clj-fst.utils])
  (:import (org.apache.lucene.util.fst BytesRefFSTEnum
                                       IntsRefFSTEnum
                                       Util)
           (org.apache.lucene.util BytesRef BytesRefBuilder
                                   IntsRef IntsRefBuilder)
           (org.apache.lucene.util.clj CljUtils)))

;; ## FST Creation
;;
;; Before creating an enumeration, you have to have a FST. Once your FST
;; is created, you can create its enumeration instance. Creating an enum
;; is as simple as:
;;
;;     (def enum (create-enum! fst))
;; 

(defn create-enum!
  "Create an enumeration of all the <input, output> tuples that
  compose the FST"
  [fst]
  (when fst (new IntsRefFSTEnum fst)))

;; ## Iterating & Searching the Enumeration
;;
;; There are two things you can do with the Enumerations API:
;;
;;   1. Iterating over the content of the FST
;;   2. Searching over the content of the FST
;;

;; ### Iterating the content of a FST
;;
;; To iterate the content of the FST, you have to use the
;; `(next!)` function. What this function does is to return
;; the next `<input,output>` tuple of the FST:
;;
;;     (next! enum)
;;
;; will return the tuple in the form of a Clojure map:
;;
;;     {:input "some input", :output ["some output"]}
;;
;; then you can always use the `(current!)` function to
;; return the current tuple pointed by the internal enumeration
;; pointer without moving it to the next tuple:
;;
;;     (current! enum)
;;
;; 

(defn current!
  "Returns the term of the current input of the enumeration"
  [enum]
  (when enum
    (let [input-output (.current enum)
          input (CljUtils/inputToString (.input input-output) true)
          output (process-output (.output input-output))]
      {:input input :output (if (vector? output) output [output])})))

(defn next!
  "Returns the term of the next input of the enumeration"
  [enum]
  (when enum
    (let [input-output (.next enum)
          input (CljUtils/inputToString (.input input-output) true)
          output (process-output (.output input-output))]
      {:input input :output (if (vector? output) output [output])})))

;; ### Searching the content of a FST
;;
;; Three functions will let you search the content of a FST: `(get-ceil-term!)`,
;; `(get-floor-term!)`, `(get-exact-term!)`. These functions will search the
;; FST in different ways. They will move the internal pointer to whatever input
;; they find. This means that if you use one of these functions, then if you use
;; the `(current!)` function than the same result will be returned because the
;; internal pointer got moved.

(defn get-ceil-term!
  "Returns the smallest term that is greater or equal to the input term, nil otherwise."
  [input enum]
  (when (and enum input)
    (let [input-output (.seekCeil enum (. Util toUTF16 input (ints-ref-builder)))]
      (if input-output
        (let [input (CljUtils/inputToString (.input input-output) true)
              output (process-output (.output input-output))]
          {:input input :output output})))))

(defn get-floor-term!
  "Returns the biggest term that is smaller or equal to the input term, nil otherwise."
  [input enum]
  (when (and enum input)
    (let [input-output (.seekFloor enum (. Util toUTF16 input (ints-ref-builder)))]
      (if input-output
        (let [input (CljUtils/inputToString (.input input-output) true)
              output (process-output (.output input-output))]
          {:input input :output (if (vector? output) output [output])})))))

(defn get-exact-term!
  "Returns the term if the exact input term exists, nil otherwise."
  [input enum]
  (when (and enum input)
    (let [input-output (.seekExact enum (. Util toUTF16 input (ints-ref-builder)))]
      (if input-output
        (let [input (CljUtils/inputToString (.input input-output) true)
              output (process-output (.output input-output))]
          {:input input :output (if (vector? output) output [output])})))))
