
(ns civbuilder.deck
  (:require  [civbuilder.common :as common]))


(defn make-card
  [name description]
  {:name name :description description })


(def ^:const emigrate 
  (make-card "Emigrate" "Place a village into an adjacent square" ))
(def ^:const farm
  (make-card "Farm" "Gain 1 food"))
(def ^:const well
  (make-card "Well" "Gain 1 water"))
(def ^:const storehouse
  (make-card "Storehouse" "Store 1 or 2 resources"))
(def ^:const famine
  (make-card "Famine" "Pay 3 food"))
    

(def available-cards
  (list emigrate farm well storehouse famine))


(defn get-initial-deck
  []
  (concat 
    (repeat 5 farm)
    (repeat 2 storehouse)
    (repeat 2 emigrate)
    (repeat 1 famine)))

(defn get-hand
  "returns [hand remaining-deck discards]"
  [deck discards num-cards]
  (if (< (count deck) num-cards)
    (let [extra-needed (- num-cards (count deck))
          [to-hand new-deck] (split-at extra-needed (shuffle discards))
          hand (concat deck to-hand)]
     [hand new-deck '()] )
    (conj (split-at num-cards (shuffle deck)) discards)))



;=============================================================
;

(defn make-cards
  [card-draw]
  (let [initial (get-initial-deck)
        [hand deck discards] (get-hand initial '() card-draw)]
    {:hand hand
     :discards discards
     :deck deck}))


(defn count-card
  [cards card-to-count]
  (let [all (concat (:hand cards) (:discards cards) (:deck cards))]
    (reduce #(if (= %2 card-to-count) (inc %1)  %1) 0 all)))



