(ns civbuilder.state
  (:require [civbuilder.grid :as grid]
            [civbuilder.deck :as deck]
            [civbuilder.common :as common]))



(defn make-state
  [name text next-func]
  (fn [action]
    (cond (= action :name) name
          (= action :text) text
          (= action :next) (next-func))))



(def start-play)

(def place-first-village 
  (make-state 
    :place-first-village 
    "Place first village"
    (fn [] start-play)))

(def start-play
  (make-state
    :start-play
    "Play a card from your hand or buy a card"
    (fn [] start-play)))


(defn initial-state
  []
  place-first-village)
