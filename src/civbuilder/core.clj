(ns civbuilder.core
  (:import (javax.swing JFrame Timer)
           (java.awt.event KeyListener ActionListener))
  (:require [civbuilder.gui :as gui]
            [civbuilder.tileset :as tileset]
            [civbuilder.common :as common]))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))


(defn civ-window
  []

  (def frame (doto (JFrame. "Rise of the Ancients")
               (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE )
              ))

  (let [insets (.getInsets frame)
        pnl (gui/civ-panel)
        [display-width display-height] (common/get-property :display-size)]
      (.setContentPane frame pnl)
      (.validate frame)
      (.repaint frame)
      (.setVisible frame true)
      (.setSize frame (+ (.left insets) (.right insets) display-width ) 
                      (+ (.top insets) (.bottom insets) display-height  )))

    )

(defn main
  []
  (common/load-common-properties)
  (civ-window))

