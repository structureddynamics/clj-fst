(defproject clj-fst "0.1.1"
  :description "Finite State Transducers (FST) for Clojure"
  :url "https://github.com/structureddynamics/clj-fst"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.apache.lucene/lucene-core "4.10.4"]
                 [org.apache.lucene/lucene-misc "4.10.4"]
                 [lein-marginalia "0.8.0"]]
  :source-paths      ["src/clojure"]
  :java-source-paths ["src/java"]
  :target-path "target/%s"
  :marginalia {:exclude ["utils.clj"]})
