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

(defn translate-mouse-coords
  [x y tile-width tile-height]
    (let [tile-x (quot x tile-width)
         tile-y (quot y tile-height)]
    [tile-x tile-y])) 


(defn translate-cell-to-coord
  [cell-x cell-y tile-width tile-height]
    [(* cell-x tile-width) (* cell-y tile-height)])

(defn draw-text
  [^java.awt.Graphics2D g2d text x y1 color font-family size]
    (.setColor g2d color)
    (.setFont g2d (Font. font-family (. Font PLAIN) size))
      (let [text-height (get-text-height g2d) 
            y (+ text-height y1 )]
       (.drawString g2d text x y)))

;=========================================================================
; 
(defn civ-panel
  []
  (let [[world-width world-height] (common/get-property :world-size)
        [tile-width tile-height] (common/get-property :tile-size)
        world (grid/make-world world-width world-height)
        tileset (tileset/load-tileset (common/get-property :tileset-def) (common/get-property :tileset-file))
        ]
    (defn handle-click
      [x y btn]
      (prn x y btn))

    (defn handle-mouse-over
      [x y]
      (prn x y))

    (doto 
      (proxy [javax.swing.JPanel] []
        (paintComponent [^java.awt.Graphics g]
          (proxy-super paintComponent g)
          (let [g2d (doto ^java.awt.Graphics2D 
                        (.create g))]
            (grid/for-each-cell 
              world
              (fn [cell]
                (let [[ix iy] (translate-cell-to-coord (:x cell) (:y cell) tile-width tile-height)
                      image (:plains tileset)]
                  (.drawImage g2d image ix iy this))))
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
                           )))))

