(ns civbuilder.gui
  (:import (javax.swing JPanel Timer )
           javax.imageio.ImageIO
           (java.awt.event ActionListener MouseListener MouseMotionListener MouseEvent KeyListener)
           (java.awt Color RenderingHints Toolkit Image Font))
  (:require [civbuilder.tileset :as tileset]
            [civbuilder.grid :as grid]
            [civbuilder.common :as common]))

;========================================================================
; General Functions
;
;


(defn load-font
  [font-file size]
  (let [load-class (.getClass (Thread/currentThread))
        unsized (Font/createFont Font/TRUETYPE_FONT (.getResourceAsStream load-class (str "/" font-file)))]
    (.deriveFont unsized (float size))))

(defn get-text-height
  [^java.awt.Graphics2D g2d]
  (let [frc (.getFontRenderContext g2d)
        lm (.getLineMetrics (.getFont g2d), "A", frc )]
    (int (.getAscent lm))))


(defn mouse-event-data
  [e]
  (let [btn (.getButton e)
       x (.getX e)
       y (.getY e)
       btn-key (cond (= btn MouseEvent/BUTTON1) :left
                    (= btn MouseEvent/BUTTON3) :right
                    :else btn)]
  [x y btn-key])) 


(defn translate-coord-to-cell
  [coord-x coord-y tile-width tile-height tile-offset-x tile-offset-y]
  [ (quot (- coord-x tile-offset-x)  tile-width) (quot (- coord-y tile-offset-y) tile-height)])

(defn translate-cell-to-coord
  [cell-x cell-y tile-width tile-height tile-offset-x tile-offset-y]
    [(+ (* cell-x tile-width) tile-offset-x) (+ (* cell-y tile-height) tile-offset-y)])

(defn draw-text
  [^java.awt.Graphics2D g2d text x y1 color font]
    (.setColor g2d color)
    (.setFont g2d font)
      (let [text-height (get-text-height g2d) 
            y (+ text-height y1 )]
       (.drawString g2d text x y)))

;=========================================================================
; 
(defn civ-panel
  []
  (let [[world-width world-height] (common/get-property :world-size)
        [tile-width tile-height] (common/get-property :tile-size)
        [tile-offset-x tile-offset-y] (common/get-property :map-offset)
        [card-tile-width card-tile-height] (common/get-property :card-tile-size)
        [hand-offset-x hand-offset-y] (common/get-property :hand-offset)
        world (grid/make-world world-width world-height)
        card-font (load-font (common/get-property :card-font) (common/get-property :card-font-size))
        hover-cell (atom nil)
        map-tileset (tileset/load-tileset (common/get-property :tileset-def) 
                                          (common/get-property :tileset-file) 
                                          (common/get-property :tile-size))
        card-tileset (tileset/load-tileset (common/get-property :card-tileset-def) 
                                           (common/get-property :card-tileset-file)
                                           (common/get-property :card-tile-size))
        handle-click (fn [x y btn]
                        (prn x y btn))

        handle-mouse-over (fn  [x y]
                            (let [[cell-x cell-y] (translate-coord-to-cell x y tile-width tile-height tile-offset-x tile-offset-y)]
                              (if (grid/valid? world cell-x cell-y)
                                (reset! hover-cell [cell-x cell-y])
                                (reset! hover-cell nil))))
        proxy-panel  (doto 
                (proxy [javax.swing.JPanel] []
                  (paintComponent [^java.awt.Graphics g]
                    (proxy-super paintComponent g)
                    (let [g2d (doto ^java.awt.Graphics2D 
                                  (.create g))]
                      (.setRenderingHint g2d RenderingHints/KEY_TEXT_ANTIALIASING 
                                         RenderingHints/VALUE_TEXT_ANTIALIAS_ON)
                      (grid/for-each-cell 
                        world
                        (fn [cell]
                          (let [[ix iy] (translate-cell-to-coord (:x cell) (:y cell) tile-width tile-height tile-offset-x tile-offset-y)
                                image ((:terrain cell) map-tileset)]
                            (.drawImage g2d image ix iy this)
                            (when (not (nil? @hover-cell))
                              (let [[h-x h-y] @hover-cell]
                                (when (and (= h-x (:x cell)) (= h-y (:y cell)))
                                  (.drawImage g2d (:hover map-tileset) ix iy this))))
                            )))

                      (let [[ix iy] (translate-cell-to-coord 0 0 card-tile-width card-tile-height hand-offset-x hand-offset-y)]
                        (.drawImage g2d (:mini-card-left card-tileset) ix iy this))

                      (draw-text g2d "Hello There" 10 10 Color/BLACK card-font)
                      )))
                (.addMouseListener (proxy [MouseListener] []
                                     (mouseClicked [e] )
                                     (mouseEntered [e] )
                                     (mouseExited [e] )
                                     (mousePressed [e] )
                                     (mouseReleased [e] 
                                       (let [[x y btn] (mouse-event-data e)]
                                                  (handle-click x y btn)))
                                     ))
                (.addMouseMotionListener (proxy [MouseMotionListener] []
                                     (mouseDragged [e])
                                     (mouseMoved [e] 
                                       (let [[x y btn] (mouse-event-data e)]
                                         (handle-mouse-over x y)))
                                     ))) 
        timer (Timer. (common/get-property :repaint-milliseconds) 
                           (proxy [ActionListener] []
                              (actionPerformed [event] 
                               (.repaint proxy-panel))))
        ]



  [proxy-panel timer] ))



(defn main
  []
  (prn (load-font "Caudex-Regular.ttf")))
