(ns civbuilder.gui
  (:import (javax.swing JPanel Timer )
           javax.imageio.ImageIO
           (java.awt.event ActionListener MouseListener MouseMotionListener MouseEvent KeyListener)
           (java.awt Color RenderingHints Toolkit Image Font))
  (:require [civbuilder.tileset :as tileset]
            [civbuilder.state :as state]
            [civbuilder.grid :as grid]
            [civbuilder.deck :as deck]
            [civbuilder.player :as player]
            [civbuilder.common :as common]))

;========================================================================
; General Functions
;
;


(defn load-font
  ([font-file style size]
    (let [load-class (.getClass (Thread/currentThread))
        unsized (Font/createFont Font/TRUETYPE_FONT (.getResourceAsStream load-class (str "/" font-file)))]
      (.deriveFont unsized style (float size))))
  ([font-file size]
   (load-font font-file Font/PLAIN size)))

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
;

;(def hand-card-image-keys '( :mini-card-left :mini-card-center :mini-card-center :mini-card-right))
(def hand-card-image-keys '( :mini-card-left :mini-card-center  :mini-card-right))
(def hand-card-width (count hand-card-image-keys))
;these should have the same count as the hand-card-image-keys
(def hand-card-full-top-image-keys '( :full-card-top-left :full-card-top-center :full-card-top-right))
(def hand-card-full-middle-image-keys '( :full-card-middle-left :full-card-middle-center :full-card-middle-right))
(def hand-card-full-bottom-image-keys '( :full-card-bottom-left :full-card-bottom-center :full-card-bottom-right))
(def sign-image-keys '( :sign-left :sign-center :sign-right))
(def sign-hover-image-keys '( :sign-hover-left :sign-hover-center :sign-hover-right))
(defn civ-panel
  []
  (let [[world-width world-height] (common/get-property :world-size)
        [tile-width tile-height] (common/get-property :tile-size)
        [tile-offset-x tile-offset-y] (common/get-property :map-offset)
        [card-tile-width card-tile-height] (common/get-property :card-tile-size)
        [hand-offset-x hand-offset-without-map-y] (common/get-property :hand-offset)
        hand-offset-y (+ hand-offset-without-map-y (* world-height tile-height) tile-offset-y)
        [sign-offset-without-map-x sign-offset-y] (common/get-property :sign-offset)
        sign-offset-x (+ sign-offset-without-map-x (* world-width tile-width) tile-offset-x)
        [sign-text-offset-x sign-text-offset-y] (common/get-property :sign-text-offset)
        [card-text-offset-x card-text-offset-y] (common/get-property :mini-card-text-offset)
        card-font (load-font (common/get-property :card-font) (common/get-property :card-font-size))
        card-bold-font (load-font (common/get-property :card-font) Font/BOLD (common/get-property :card-font-size))
        sign-font (load-font (common/get-property :sign-font) (common/get-property :sign-font-size))
        sign-bold-font (load-font (common/get-property :sign-font) Font/BOLD (common/get-property :sign-font-size))
        message-font (load-font (common/get-property :message-font) (common/get-property :message-font-size))
        [message-offset-x message-offset-y-raw] (common/get-property :message-offset)
        message-offset-y (+ message-offset-y-raw hand-offset-y card-tile-height)
        world (atom (grid/make-world world-width world-height))

        card-draw (common/get-property :card-draw)
        cards (atom (deck/make-cards card-draw))
        hover-cell (atom nil)
        hover-card (atom nil) ;leftmost cell of card display
        hover-sign (atom nil)
        current-state (atom (state/initial-state))
        message-text (atom "Hi There")
        active-cells (atom nil)
        player-stuff (atom (player/make-player))
        
        signs (concat player/resource-keys
                      '(nil nil)
                      deck/available-cards
                      '(nil nil)
                      '("End Turn")
                      )
        
        sign-spacer 2
        map-tileset (tileset/load-tileset (common/get-property :tileset-def) 
                                          (common/get-property :tileset-file) 
                                          (common/get-property :tile-size))
        card-tileset (tileset/load-tileset (common/get-property :card-tileset-def) 
                                           (common/get-property :card-tileset-file)
                                           (common/get-property :card-tile-size))
        
        hand-cells-available (quot (- (first (common/get-property :display-size)) hand-offset-x) card-tile-width) ]

        (defn update
          []
          (reset! message-text (@current-state :text)))


        (defn position-hand-cards
          "Return a sequence with count equal to card count.
          Each item in the seq will be the [min-cell max-cell] of the corresponding card"
          [card-count cells-available]
          (if (= 0 card-count)
            '()
            (let [cells-per-card (min hand-card-width (quot cells-available card-count))
                  spacing (if (> (quot cells-available card-count) hand-card-width) 1 0)
                  _ (assert (> cells-per-card 0) "Too many cards to display")]
              (for [c (range card-count)]
                (let [min-cell (+ (* spacing c) (* c cells-per-card))
                      max-cell (+ min-cell (dec cells-per-card)  )]
                  [min-cell max-cell])))))




        (defn draw-card-section
          [g2d min-cell cell-y-offset image-keys watcher]
          (let [pos-list (map #(vector (+ min-cell %1) %2) (range (count image-keys)) image-keys)]
            (doseq [[pos img-key] pos-list]
              (let [[ix iy] (translate-cell-to-coord pos cell-y-offset 
                                                     card-tile-width card-tile-height 
                                                     hand-offset-x hand-offset-y)]
                (.drawImage g2d (img-key card-tileset) ix iy watcher)
                ))))

        (defn draw-card-text
          [g2d card min-cell cell-y-offset]
          (let [[ix iy] (translate-cell-to-coord min-cell cell-y-offset
                                                 card-tile-width card-tile-height 
                                                 hand-offset-x hand-offset-y)]
            (draw-text g2d (:name card) (+ ix card-text-offset-x) (+ iy card-text-offset-y) Color/BLACK card-font)))

        (defn draw-hand-card 
          [g2d card min-cell max-cell watcher]
          (if (= @hover-card min-cell)
            ;-- Show full card
            (do
              (draw-card-section g2d min-cell -2 hand-card-full-top-image-keys watcher)
              (draw-card-section g2d min-cell -1 hand-card-full-middle-image-keys watcher)
              (draw-card-section g2d min-cell 0 hand-card-full-bottom-image-keys watcher)
              (draw-card-text g2d card min-cell -2))
            ;--- Show mini card
            (do
              (draw-card-section g2d min-cell 0 hand-card-image-keys watcher)
              (draw-card-text g2d card min-cell 0))
            ))

        (defn draw-hand-cards
          [g2d hand-cards watcher]
          (let [card-positions (position-hand-cards (count hand-cards) hand-cells-available)
                cards-with-pos (map #(vector %1 %2) hand-cards card-positions)]
              (doseq [[card pos] cards-with-pos]
                (let [[min-cell max-cell] pos]
                  (draw-hand-card g2d card min-cell max-cell watcher)))))


        (defn get-card-for-cell
          [min-cell hand-cards]
          (let [card-positions (position-hand-cards (count hand-cards) hand-cells-available)
                cards-with-pos (map #(vector %1 %2) hand-cards card-positions)
                card-for-cell (filter (fn [[card [i-min i-max]]] (= min-cell i-min)) cards-with-pos)]
            (when (not-empty card-for-cell)
              (let [[card pos] (first card-for-cell)]
                card))))
            
        (defn draw-message
          [g2d watcher]
          (when-let [msg @message-text]
            (draw-text g2d msg message-offset-x message-offset-y Color/BLACK message-font)))

        (defn draw-sign
          [g2d cell-y-offset text watcher]
          (let [image-keys (if (= cell-y-offset @hover-sign) sign-hover-image-keys sign-image-keys)
                pos-list (map #(vector %1 %2) (range (count image-keys)) image-keys)]
            (doseq [[pos img-key] pos-list]
              (let [[ix iy] (translate-cell-to-coord pos cell-y-offset
                                                     card-tile-width card-tile-height
                                                     sign-offset-x sign-offset-y)]
                (.drawImage g2d (img-key card-tileset) ix iy watcher)
              ))
            (let [[ix iy] (translate-cell-to-coord 0 cell-y-offset
                                                   card-tile-width card-tile-height
                                                   sign-offset-x sign-offset-y)]
              (draw-text g2d text (+ ix sign-text-offset-x) (+ iy sign-text-offset-y) Color/BLACK sign-font))
            ))


        (defn draw-signs
          [g2d watcher]
          (doseq [[idx sign-item] (map-indexed #(vector %1 %2) signs)]
            (when sign-item
              (cond (some #(= % sign-item) player/resource-keys)
                    (let [sign-text  (format "%-7s %4d" (player/resource-name sign-item) (sign-item @player-stuff)) ]
                      (draw-sign g2d idx sign-text watcher))
                    (some #(= % sign-item) deck/available-cards)      
                    (draw-sign g2d idx (:name sign-item) watcher)
                    :else
                    (draw-sign g2d idx sign-item watcher)
              ))))




        (defn translate-event-to-world
          [x y ]
          ;attempt to map to a square on the map
          (let [[cell-x cell-y] (translate-coord-to-cell x y tile-width tile-height tile-offset-x tile-offset-y)]
            (when (grid/valid? @world cell-x cell-y)
              [:world cell-x cell-y ])))

        (defn translate-event-to-hand-card
          [x y cards-in-hand]
          (let [[card-x card-y] (translate-coord-to-cell x y card-tile-width card-tile-height
                                                         hand-offset-x hand-offset-y)
                card-positions (position-hand-cards cards-in-hand hand-cells-available)]
            (when (= card-y 0)
              (let [hovered-card-pos (filter (fn [[min-cell max-cell]] 
                                                (and (>= card-x min-cell) (<= card-x max-cell)))
                                                card-positions)]
                (when (> (count hovered-card-pos) 0)
                  (let [[min-cell max-cell] (first hovered-card-pos)]
                    [:hand min-cell 0] ))))))


        (defn translate-event-to-sign
          [x y]
          (let [[cell-x cell-y] (translate-coord-to-cell x y card-tile-width card-tile-height
                                                         sign-offset-x sign-offset-y)]
            [:sign cell-x cell-y]))

        (defn translate-event
          "returns [cell-x cell-y <:world or :hand>] or nil if no matching cell found" 
          [x y cards-in-hand]

          (or (translate-event-to-world x y)
              (translate-event-to-hand-card x y cards-in-hand)
              (translate-event-to-sign x y)
              ))


        (defn handle-click 
          [x y btn cards-in-hand]
          (when-let [cell-event (translate-event x y cards-in-hand)]
            (let [[event-type cell-x cell-y] cell-event]
              (cond (= event-type :world)
                    (do
                      (prn (str "Clicked on cell " cell-x "," cell-y))
                      (case (@current-state :name)
                        :place-first-village 
                          (do 
                            (prn "placing village")
                            (swap! current-state #(% :next) )
                            (swap! world (grid/add-village cell-x cell-y))
                            )
                        :start-play
                          (do
                            (prn "start play")
                            )
                        ))
                    (= event-type :hand)
                    (do
                      (let [card (get-card-for-cell cell-x (:hand @cards))]
                        (prn (str "Clicked on card") card)))
                    (= event-type :sign)
                    (do
                      (when (<= cell-y (count signs))
                        (prn (nth signs cell-y))))
                    ))))

        (defn handle-mouse-over 
          [x y cards-in-hand]
          (reset! hover-cell nil)
          (reset! hover-card nil)
          (reset! hover-sign nil)
          (when-let [cell-event (translate-event x y cards-in-hand)]
            (let [[event-type cell-x cell-y] cell-event]
              (cond (= event-type :hand)
                    (reset! hover-card cell-x)
                    (= event-type :world)
                    (reset! hover-cell [cell-x cell-y])
                    (= event-type :sign)
                    (reset! hover-sign cell-y)
                    ))))

        (let [proxy-panel  (doto 
                (proxy [javax.swing.JPanel] []
                  (paintComponent [^java.awt.Graphics g]
                    (proxy-super paintComponent g)
                    (let [g2d (doto ^java.awt.Graphics2D 
                                  (.create g))]
                      (.setRenderingHint g2d RenderingHints/KEY_TEXT_ANTIALIASING 
                                         RenderingHints/VALUE_TEXT_ANTIALIAS_ON)
                      (update)
                      (grid/for-each-cell 
                        @world
                        (fn [cell]
                          (let [[ix iy] (translate-cell-to-coord (:x cell) (:y cell) tile-width tile-height tile-offset-x tile-offset-y)
                                image ((:terrain cell) map-tileset)]
                            (.drawImage g2d image ix iy this)
                            (when-let [village (:village cell)]
                              (.drawImage g2d (:village map-tileset) ix iy this))
                            (when (and (= :place-first-village (@current-state :name))
                                       (= (:terrain cell) :plains))
                              (.drawImage g2d (:select map-tileset) ix iy this)) 
                               
                            (when (not (nil? @hover-cell))
                              (let [[h-x h-y] @hover-cell]
                                (when (and (= h-x (:x cell)) (= h-y (:y cell)))
                                  (.drawImage g2d (:hover map-tileset) ix iy this))))
                            )))
                      (draw-hand-cards g2d (:hand @cards) this)
                      (draw-message g2d this)
                      (draw-signs g2d this)
                      )))
                (.addMouseListener (proxy [MouseListener] []
                                     (mouseClicked [e] )
                                     (mouseEntered [e] )
                                     (mouseExited [e] )
                                     (mousePressed [e] )
                                     (mouseReleased [e] 
                                       (let [[x y btn] (mouse-event-data e)]
                                                  (handle-click x y btn (count (:hand @cards)))))
                                     ))
                (.addMouseMotionListener (proxy [MouseMotionListener] []
                                     (mouseDragged [e])
                                     (mouseMoved [e] 
                                       (let [[x y btn] (mouse-event-data e)]
                                         (handle-mouse-over x y (count (:hand @cards)))))
                                     ))) 
              timer (Timer. (common/get-property :repaint-milliseconds) 
                           (proxy [ActionListener] []
                              (actionPerformed [event] 
                               (.repaint proxy-panel))))
        ]

      [proxy-panel timer] )))



(defn main
  []
  (prn (load-font "Caudex-Regular.ttf")))
