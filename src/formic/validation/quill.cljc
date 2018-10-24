(ns formic.validation.quill
  (:require [clojure.string :as str]))

(def validate-required
  {:message "This field is required"
   :validate (fn [m]
               (when (and (map? m) (:txt m))
                 (not (str/blank? (:txt m)))))})
