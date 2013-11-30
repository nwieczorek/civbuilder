
(ns civbuilder.grid
  (:require [civbuilder.common :as common]))


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
  "Return an updated grid
  match-func is a function with signature (f x y) that returns true if the cell should be updated
  update-func is a function with signature (f cell) that returns the updated cell"
  ([grid match-func update-func]
    (let [updater (fn [ex ey]
                    (let [ecell (get-cell grid ex ey)]
                      (if (match-func ex ey)
                        (update-func ecell)
                        ecell)))]
      (assoc grid :cells (make-grid-cells (:width grid) (:height grid) updater))))
  ([grid x y update-func]
    (let [match-func #(and (= x %1) (= y %2))]
      (update-grid grid match-func update-func))))

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


(defn for-each-cell
  [grid func]
  (doseq [row (:cells grid)]
    (doseq [cell row]
      (func cell))))


;==========================================================
;

(defn make-world
  [width height]
  (let [mountain-seed (common/get-property :mountain-seed-chance)
        water-seed (common/get-property :water-seed-chance)
        forest-seed (common/get-property :forest-seed-chance)
        total-seed (+ mountain-seed water-seed forest-seed)]
  (update-grid
    (make-grid width height {:terrain :plains})
    (fn [x y]
      (< (rand-int 100) total-seed))
    (fn [cell]
      (let [r (rand-int total-seed)
            new-terrain (cond (< r mountain-seed)
                              :mountain
                              (< r (+ mountain-seed forest-seed))
                              :forest
                              (<  r (+ mountain-seed forest-seed water-seed))
                              :water
                              :else
                              :plains)]
        (assoc cell :terrain new-terrain))))))


