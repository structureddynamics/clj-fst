(defproject clj-fst "0.1.1"
  :description "Finite State Transducers (FST) for Clojure"
  :url "https://github.com/structureddynamics/clj-fst"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.apache.lucene/lucene-core "7.3.1"]
                 [org.apache.lucene/lucene-misc "7.3.1"]
                 [lein-marginalia "0.9.1"]]
  :source-paths      ["src/clojure"]
  :java-source-paths ["src/java"]
  :target-path "target/%s"
  :marginalia {:exclude ["utils.clj"]})
