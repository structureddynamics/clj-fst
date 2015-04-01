(ns clj-fst.utils)

(defn process-output
  "Process the output of an FST enumeration to generate a vector of outputs"
  [output]
  (let [output (if (instance? java.util.ArrayList output)
                 (into [] output)
                 [output])]
    (->> output
         (map (fn [c-ref]
                [(.toString c-ref)]))
         (apply concat)
         (into []))))              
