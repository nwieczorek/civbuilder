
(ns civbuilder.grid
  (:require [civ.common :as common]))


(defn make-grid-cells
  [width height maker-func]
  (into 
    [] 
    (for [y (range height)]
      (into
        []
        (for [x (range width)]
          (maker-func x y))))))


(defn make-grid
  [width height initial-cell]
  { :width width :height height
   :cells  (make-grid-cells width height #(assoc initial-cell :x %1 :y %2  )) 
   })

(defn get-cell
  [grid x y]
    (let [cells (grid :cells)]
      ((cells y) x)))

(defn update-grid
  [grid x y update-func]
  (let [updater (fn [ex ey]
                  (let [ecell (get-cell grid ex ey)]
                    (if (and (= ex x) (= ey y))
                      (update-func ecell)
                      ecell)))]
    (assoc grid :cells (make-grid-cells (grid :width) (grid :height) updater))))



(defn valid?
  [grid x y]
  (let [height (grid :height)
        width (grid :width)]
    (and (>= x 0) (>= y 0)
         (< y height) (< x width))))

(def orthogonal [ [-1 0] [1 0] [0 -1] [0 1]])

(defn add-coords
  [[x1 y1] [x2 y2]]
  [(+ x1 x2) (+ y1 y2)])

(defn get-adjacent-cells
  "returns the orthogonal adjacent cells"
  [grid x y]
  (let [valid-coords (filter #(valid? grid (% 0) (% 1)) 
                           (map #(add-coords % [x y]) orthogonal))]
    (map (fn [[ix iy]] (get-cell grid ix iy)) valid-coords)))

