
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

(defn match-and-update-grid
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
      (match-and-update-grid grid match-func update-func))))


(defn update-grid
  "Update cell or cells in the grid
  update-func returns nil if no update is to be made to that cell, the update cell otherwise"
  [grid update-func]
  (let [updater (fn [ex ey]
                  (let [ecell (get-cell grid ex ey)]
                    (if-let [new-cell (update-func ex ey ecell)]
                      new-cell
                      ecell)))]
    (assoc grid :cells (make-grid-cells (:width grid) (:height grid) updater))))

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


(defn filter-cells
  "return a sequence of cells where the predicate is true"
  [grid func]
  (filter func (flatten (:cells grid))))


;==========================================================
;

(defn make-world
  [width height]
  (let [mountain-seed (common/get-property :mountain-seed-chance)
        water-seed (common/get-property :water-seed-chance)
        forest-seed (common/get-property :forest-seed-chance)
        seed-map { :mountain mountain-seed
                  :forest forest-seed
                  :water water-seed }
        mountain-growth (common/get-property :mountain-growth-chance)
        water-growth (common/get-property :water-growth-chance)
        forest-growth (common/get-property :forest-growth-chance)
        growth-iterations (common/get-property :map-growth-iterations)
        growth-map { :mountain mountain-growth
                    :forest forest-growth
                    :water water-growth}

        total-seed (+ mountain-seed water-seed forest-seed)
        seeded-grid (update-grid
                        (make-grid width height {:terrain :plains :village nil})
                        (fn [x y cell]
                          (let [seeded-cells (keep identity 
                                               (for [seed-key (keys seed-map)]
                                                (let [r (rand-int 100)]
                                                  (when (< r (seed-key seed-map))
                                                    (assoc cell :terrain seed-key)))))]
                            (when (not-empty seeded-cells)
                              (first seeded-cells))))
                      )]





     (loop [counter 0
            curr-grid seeded-grid]

           (defn world-growth-update
             [x y cell]
             (when (= :plains (:terrain cell))
               (let [adj (get-adjacent-cells curr-grid x y)
                     grown-cells (keep identity
                                       (for [gr-key (keys growth-map)]
                                         (when (not-empty (filter #(= gr-key (:terrain %)) adj))
                                           (let [r (rand-int 100)]
                                             (when (< r (gr-key growth-map))
                                               (assoc cell :terrain gr-key))))))]
                (when (not-empty grown-cells) (first grown-cells)))))

       (if (< counter growth-iterations)
         (recur (inc counter) 
                 (update-grid curr-grid world-growth-update))
         curr-grid))))
           
       
(defn add-village
  "Returns a function that takes the grid as an argument"
  [x y]
  (fn [grid]
    (update-grid grid (fn [ix iy cell]
                        (when (and (= x ix) (= y iy))
                          (assoc cell :village :new-village))))))


