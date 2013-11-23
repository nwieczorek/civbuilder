(ns civbuilder.core
  (:import (javax.swing JFrame Timer)
           (java.awt.event KeyListener ActionListener))
  (:require [civ.tileset :as tileset]
            [civ.common :as common]))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))


(defn civ-window
  []

  (def frame (doto (JFrame. "Rise of the Ancients")
               (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE )
              ))

  ;(.setContentPane frame p)
  (.validate frame)
  (.repaint frame)
  (.setVisible frame true)
  (let [insets (.getInsets frame)]
      (.setSize frame (+ (.left insets) (.right insets) (common/get-property :display-width) ) 
                      (+ (.top insets) (.bottom insets) (common/get-property :display-height ) )))

    )

(defn main
  []
  (common/load-common-properties)
  (civ-window))

