
(ns civbuilder.player
  (:require [civbuilder.common :as common]))


(def resource-keys
  (seq '(:food :wood :stone :water)))

(def resource-names
  {:food "Food"
   :wood "Wood"
   :stone "Stone"
   :water "Water"})

(defn resource-name
  [res-key]
  (res-key resource-names))

(defn make-player
  []
  { :food (common/get-property :initial-food)
    :wood (common/get-property :initial-wood)
    :stone (common/get-property :initial-stone)
    :water (common/get-property :initial-water) 
   })

(defn update-resource
  [player resource-key amount]
  (assoc player resource-key (+ (resource-key player) amount)))
