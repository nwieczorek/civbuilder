(ns civbuilder.tileset
  (:import (java.io BufferedReader FileReader InputStreamReader)
           javax.imageio.ImageIO)
  (:require [civbuilder.common :as common]))




(defn get-subimage
  [full-image tile-x tile-y tile-width tile-height kw]
    (let [x (* tile-x tile-width)
          y (* tile-y tile-height)
          full-height (.getHeight full-image)
          full-width (.getWidth full-image)]
      (assert (and (< (+ x tile-width) full-width) (< (+ y tile-height) full-height))
              (str "Subimage " kw " at " tile-x "," tile-y "(" x "," y ")" 
                   " outside " full-width "," full-height))
    (.getSubimage full-image x y tile-width tile-height)))



(defn get-image-tiler
  [full-image tile-def]
  (let [[tile-width tile-height] (common/get-property :tile-size)]
    (fn [image-map kw]
      (let [[tile-x tile-y] (tile-def kw)
            subimg (get-subimage full-image tile-x tile-y tile-width tile-height kw )]
        (assoc image-map kw subimg)))))



(defn load-tileset
  [def-filename image-filename]
  (let [load-class (.getClass (Thread/currentThread))
        ts-def (common/load-property-file def-filename )
        tile-image-file image-filename
        _ (prn (str "loading " tile-image-file))
        full-image (ImageIO/read (.getResource load-class (str "/" tile-image-file)) )]

    (reduce (get-image-tiler full-image ts-def) {} (keys ts-def))
    ))



(defn main
  []
  (common/load-common-properties)
  (prn (load-tileset (common/get-property :tileset-def) (common/get-property :tileset-file) ))
  )
