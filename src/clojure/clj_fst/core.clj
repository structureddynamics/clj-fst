;; # Clojure Finite State Transdurer (FST)
;;
;; This Clojure FST implementation is a wrapper above the Lucene FST package which is part of Lucene core.
;;
;; Finite state transducers are finite state machines with two tapes: an input and an output tape. The automaton
;; map an input string to an output. The output can be another string or an integer.
;;
;; The FST produced by this application are implemented as a bytes array which makes FST really effective
;; indexes in terms of speed and memory consumption. In fact, a 10 millions terms index will takes roughtly
;; 256 MB of memory (depending of the string composition of the input strings, and if the outputs are
;; integers or strings).
;;
;; ## Limitations
;;
;; The main limitation is that a FST index cannot be updated once it is created. This means that it cannot
;; evolves over time. If you want to add or remove inputs/outputs, then you have to re-create the FST
;; entirely.
;;

(ns clj-fst.core
  (:use [clj-fst.utils])  
  (:refer-clojure :exclude [load])
  (:import (org.apache.lucene.util.fst PositiveIntOutputs CharSequenceOutputs ListOfOutputs Builder FST Util)
           (org.apache.lucene.util BytesRef BytesRefBuilder)
           (org.apache.lucene.util IntsRef IntsRefBuilder)
           (org.apache.lucene.util CharsRef CharsRefBuilder)))

(declare int-outputs char-outputs chars-ref chars-ref-builder ints-ref ints-ref-builder bytes-ref bytes-ref-builder builder!)

;; ## Building and Populating a FST
;; 
;; The creation of a new FST is a 3 steps process:
;;
;;  1. Create a new builder using `(create-builder!)`. The builder is used to populate the index with `<input,output>` tuples
;;  2. Populate the index using `(add!)`
;;  3. Create the FST using `(create-fst!)`
;;
;; ### Builder Creation
;;
;; The first step is to create the FST's builder by using the `(create-builder!)` function. There are two types of
;; outputs currently supported by this wrapper:
;;
;;  1. integers
;;  2. unicode strings
;;
;; Here is some code that shows you how to create the builder:
;; 
;;     ;; Create a new FST builder with `:char` outputs
;;     (def builder (create-builder! :type :char))
;;
;;     ;; Create a new FST builder with `:int` outputs
;;     (def builder (create-builder! :type :int))
;;

(defn create-builder!
  "Create a new FST builder map.

  * `[type]` *(optional)*: Output type of the FST. Can be `:int` or `:char` (default)"
  [& {:keys [type]
      :or {type :char}}]
  {:builder (builder! type)
   :type type})

;; ### Populating the FST
;;
;; Populating the FST with `<input,output>` tuples is quite simple. The only thing you have to do
;; once your builder is created, is to add the tuples iteratively by calling multiple types the
;; `(add!)` function.
;;
;; However, there is quite an important thing to keep in mind:
;;
;; **You have to sort the index you want to create by their input keys**
;;
;; If you miss to perform this step, then you will end-up with unexpected results.
;;
;; Populating a FST is quite simple. Here is a code example that will populate a FST
;; using a `sorted-map`:
;;
;;     ;; The first thing to do is to create the Builder
;;     (def builder (create-builder! :type :int))
;; 
;;     ;; This small `sorted-map` defines the things
;;     ;; to add to the FST
;;     (def values (into (sorted-map) {"cat" 1
;;                                     "dog" 2
;;                                     "mice" 3}))
;;
;;     ;; Populate the FST using that `sorted-map`
;;     (doseq [[input output] values]
;;       (add! builder {input output}))
;;
;; What this code shows you is how you can iteratively
;; populate the FST. However, if you already have a single
;; `sorted-map` that does have all the `<input,output>`
;; tuples that you want in your FST, then you can simply
;; use `(add!)` this way:
;;
;;     ;; Populate the FST using that `sorted-map`
;;     (add! builder values))
;;     
;;     

(defn add!
  "Populate a FST with `<input,output>` tuples. This function can be called iteratively
  multiple times before the `(create-fst!)` function is called to actually create the
  FST.

   * `[builder]`: builder where to populate the FST
   * `[values]`: map of the inputs->ouputs. The keys of the maps are the inputs,
                 and their values are the outputs.

  **Note:** if `(add!)` is used iteratively, then you have to make sure that the
            structure it iterates over has been previously sorted by the input keys."
  [builder values]
  (let [scratch-bytes-builder (bytes-ref-builder)
        scratch-ints (ints-ref-builder)]
    (doseq [[word index] values]      
      (case (builder :type)
          :int (do
                 (.append scratch-bytes-builder (BytesRef. word))
                 (.add (builder :builder) (. Util toIntsRef (.get scratch-bytes-builder) scratch-ints) index))
          :char (.add (builder :builder) (. Util toUTF16 word scratch-ints) (new CharsRef index))))))

;; ### FST Creation
;;
;; Once the builder is create and populated with the inputs and outputs, then the final
;; step is to create the actual FST. Once the FST is created, there is no way to add
;; or remove any inputs/outputs. If this become necessary, the FST needs to be re-created.
;;
;; Creating the FST is as simple as calling `(create-fst!)`:
;;
;;     ;; Creating a new FST
;;     (def fst (create-fst! builder))

(defn create-fst!
  "Create a new FST based on a builder that has been populated with inputs/outputs

   * `[builder]`: builder option that has been created and populated"
  [builder]
  (.finish (builder :builder)))

;; ### Querying the FST
;;
;; Now that we have a fully operational FST, the end goal is to be able to use it.
;; What we want to do is to know if there exists an output for a given input, and if
;; there is one to return the output associated with the input. Finally, if there is
;; no output for a given input, we want to get a `nil` value.
;;
;; Querying the FST is as simple as:
;;
;;     ;; Query the FST
;;     (get-output "cat" fst)
;;
;; This will return the output of the input string `"cat"`
;; 

(defn get-output
  "Get the output for a given input.

  * `[input]`: input for which we want its output
  * `[fst]`: FST object where to look for the output"
  [input fst]
  (let [result (. Util get fst (. Util toUTF16 input (ints-ref-builder)))]
    (if-not (nil? result)
      (process-output result))))

;; ### Loading and Saving FST
;;
;; It is possible to save FST on the file system. That way, it is
;; possible to reload a FST from the file system when your
;; application starts.
;;
;;     ;; Save a FST on the file system
;;     (save! "resources/fst.srz" fst)
;;
;;     ;; Load a FST from the file system
;;     (load! "resources/fst.srz)

(defn save!
  "Save a FST to a file on the file system

  * `[file]` is the file path on the file system
  * `[fst]` is the FST instance"
  [file fst]
  (. fst save (.toPath (clojure.java.io/file file))))
  
(defn load!
  "Load a FST to a file on the file system

  [file] is the file path on the file system
  [output-type] (optional) :int (default) when the output of the FST file are
                           integers. :char when the output of the FST file are
                           characters.

  Returns the loaded FST"
  [file & {:keys [output-type]
           :or {output-type :char}}]
  (let [outputs (if (= output-type :int)
                  (int-outputs)
                  (char-outputs))]
    (. FST read (.toPath (clojure.java.io/file file)) outputs)))

;; ## Utility functions
;;
;; This section list a series of utilities functions used by the
;; core `clj-fst` functions

(defn builder!
  "Create a builder object.

  You can directly use this function instead of the `(create-builder!)` function
  if you require really specific settings.

  * `[type]`: type of the output. Can be `:int` or `:char`
  * `[min-suffix-count-1]` (optional): If pruning the input graph during construction, this threshold is used for telling
                                       if a node is kept or pruned. If transition_count(node) >= minSuffixCount1, the node
                                       is kept.
  * `[mind-suffix-count-2]` (optional): (Note: only Mike McCandless knows what this one is really doing...)
  * `[share-suffix]` (optional): If true, the shared suffixes will be compacted into unique paths. This requires an
                                 additional RAM-intensive hash map for lookups in memory. Setting this parameter to
                                 false creates a single suffix path for all input sequences. This will result in a
                                 larger FST, but requires substantially less memory and CPU during building.
  * `[share-non-singleton-nodes]` (optional): Only used if share-suffix is true. Set this to true to ensure FST is
                                              fully minimal, at cost of more CPU and more RAM during building.
  * `[share-max-tail-length]` (optional): Only used if share-suffix is true. Set this to
                                          Integer.MAX_VALUE to ensure FST is fully minimal, at cost of more
                                          CPU and more RAM during building.
  * `[allow-array-arcs]` (optional): Pass false to disable the array arc optimization while building the FST;
                                     this will make the resulting FST smaller but slower to traverse.
  * `[bytes-page-bits]` (optional): How many bits wide to make each byte[] block in the BytesStore; if you know
                                    the FST will be large then make this larger. For example 15 bits = 32768
                                    byte pages."
  [type & {:keys [min-suffix-count-1
                  min-suffix-count-2
                  share-suffix
                  share-non-singleton-nodes
                  share-max-tail-length
                  pack-fst
                  acceptable-overhead-ratio
                  allow-array-arcs
                  bytes-page-bits]
           :or {min-suffix-count-1 0
                min-suffix-count-2 0
                share-suffix true
                share-non-singleton-nodes true
                share-max-tail-length Integer/MAX_VALUE
                pack-fst false
                acceptable-overhead-ratio org.apache.lucene.util.packed.PackedInts/COMPACT
                allow-array-arcs true
                bytes-page-bits 15}}]
  (if (= type :int)
    (Builder. org.apache.lucene.util.fst.FST$INPUT_TYPE/BYTE1
              min-suffix-count-1
              min-suffix-count-2
              share-suffix
              share-non-singleton-nodes
              share-max-tail-length
              (int-outputs)
              allow-array-arcs
              bytes-page-bits)
    (Builder. org.apache.lucene.util.fst.FST$INPUT_TYPE/BYTE4
              min-suffix-count-1
              min-suffix-count-2
              share-suffix
              share-non-singleton-nodes
              share-max-tail-length
              (char-outputs)
              allow-array-arcs
              bytes-page-bits)))

(defn int-outputs
  "Create a PositiveIntOutputs"
  []
  (new ListOfOutputs (. PositiveIntOutputs getSingleton)))

(defn char-outputs
  "Create a CharSequenceOutputs"
  []
  (new ListOfOutputs (. CharSequenceOutputs getSingleton)))

(defn bytes-ref
  "Create a BytesRef"
  []
  (new BytesRef))

(defn bytes-ref-builder
  "Create a BytesRefBuilder"
  []
  (new BytesRefBuilder))

(defn ints-ref
  "Create a IntsRef"
  []
  (new IntsRef))
  
(defn ints-ref-builder
  "Create a IntsRefBuilder"
  []
  (new IntsRefBuilder))

(defn chars-ref
  "Create a CharsRef"
  []
  (new CharsRef))

(defn chars-ref-builder
  "Create a CharsRefBuilder"
  []
  (new CharsRefBuilder))
