(ns civbuilder.common
  (:import (java.io BufferedReader FileReader InputStreamReader)
           javax.imageio.ImageIO))


(defn strip-colon
  "Remove the first colon found in the string"
  [s]
  (clojure.string/replace-first s ":" ""))

(defn keyword-append
  "Append two keywords or strings together separated by a colon, and return the result as a keyword"
  [kw-a kw-b]
  (keyword (str (strip-colon (str kw-a)) "-" (strip-colon (str kw-b)))))



(defn resolve-line
  "Helper function for loading properties files.
  Parse the line and add the key -> property association to the map"
  [def-map line]
  (if (clojure.string/blank? line)
    def-map
    (let [tokens (clojure.string/split line #"\s+")]
      (if (> (count tokens) 1)
        (let [kw (read-string (str ":" (first tokens)))
              tokes (map read-string (rest tokens))
              val (if (= 1 (count tokes)) (first tokes) tokes) ]
          (assoc def-map kw val))
        def-map))))


(defn load-property-file
  [filename]
  (let [load-class (.getClass (Thread/currentThread))]
    (with-open [rdr (BufferedReader. (InputStreamReader. (.getResourceAsStream load-class (str "/" filename))))]
      (reduce resolve-line {} (line-seq rdr)))))

(def ^:dynamic *common-properties* (atom nil))
(def common-property-file "common.txt")

(defn load-common-properties
  []
  (reset! *common-properties* (load-property-file common-property-file)))

(defn get-property
  [kw]
  (@*common-properties* kw))

(defn main
  []
  (prn (load-property-file common-property-file))
  )

