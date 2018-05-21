(ns formic.components.quill
  (:require
   [formic.util :as u]
   [reagent.core :as r]
   [cljsjs.quill]
   [formic.field :as field]
   [clojure.string :as s]))

(defn replace-contents [editor value]
  (let [sel (.getSelection editor)]
    (.dangerouslyPasteHTML (.-clipboard editor) (or value ""))))

(def default-options
  {:theme "snow"
   :modules {:toolbar
             ["bold"
              "italic"
              "underline"
              {:list "bullet"}
              {:indent "-1"}
              {:indent "+1"}
              "link"
              "clean"]}
   :formats ["bold" "italic" "underline" "list" "indent" "bullet" "link"]})

(defn DEFAULT_SERIALIZER [delta]
  (js->clj (.-ops delta) :keywordize-keys true))

(defn quill [f]
  (let [element (r/atom nil)
        editor (r/atom nil)
        {:keys [modules formats validation theme touched current-value err]} f]
    (r/create-class
     {:component-did-mount
      (fn [_]
        (let [options (clj->js (merge-with #(or % %2)
                                default-options
                                {:modules modules
                                 :theme theme
                                 :formats formats}))
              ed (js/Quill. @element options)]
          ;; update value on change
          (.on ed "text-change"
               (fn [delta olddelta source]
                 (reset! current-value (.getContents ed))))
          ;; ensure touched when blurred
          (set! (.. @element -firstChild -onblur)
                (fn [ev]
                  (reset! touched true)))
          (if (string? @current-value )
            (.setText ed @current-value)
            (.setContents ed @current-value))
          (reset! editor ed)))
      :component-will-unmount
      (fn [_]
        (let [ed @editor]
          (.off ed "editor-change")
          (reset! editor nil)))
      :reagent-render
      (fn [f]
        [:div.formic-quill {:class (when @err "error")}
         [:span.formic-input-title
          (u/format-kw (:id f))]
         [:div.formic-quill-editor-wrapper
          [:div {:ref (fn [el] (reset! element el))}]]
         (when @err
           [:h3.error err])])})))
