(ns formic.components.quill
  (:require
   [formic.util :as u]
   [reagent.core :as r]
   [reagent.impl.component :refer [extract-props]]
   [cljsjs.quill]
   [formic.field :as field]
   [clojure.string :as s]
   [goog.object :as gobj]))

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
  (when (and delta
             (not (string? delta))) ;; can be string on initialization
    (js->clj (gobj/get delta "ops") :keywordize-keys true)))

(defn quill [{:keys [id touched value err options]}]
  (let [{:keys [modules formats theme]} options
        element (r/atom nil)
        editor (r/atom nil)
        should-update (atom false)]
    (r/create-class
     {:display-name (str "formic-quill-" (name id))
      :component-did-mount
      (fn [_]
        (let [options (clj->js (merge-with #(or % %2)
                                           default-options
                                           {:modules modules
                                            :theme theme
                                            :formats formats}))
              ed (js/Quill. @element options)
              reset-ed-fn 
              (fn []
                (if (string? @value)
                  (.setText ed @value)
                  (.setContents ed @value)))]
          ;; update value on change
          (.on ed "text-change"
               (fn [delta olddelta source]
                 (when (= source "user")
                   (reset! value (.getContents ed))
                   (reset! should-update false))))
          ;; ensure touched when blurred
          (set! (.. @element -firstChild -onblur)
                (fn [ev]
                  (reset! touched true)))
          (reset-ed-fn)
          (reset! editor ed)))
      :component-did-update
      (fn [_ props]
        (when (and  @editor
                    @should-update)
          (let [cv @(:value (extract-props props))]
            (if (string? cv)
              (.setText @editor cv)
              (.setContents @editor (gobj/get cv "ops")))))
        (reset! should-update true))
      :component-will-unmount
      (fn [_]
        (let [ed @editor]
          (.off ed "editor-change")
          (reset! editor nil)))
      :reagent-render
      (fn [{:keys [id touched value err options]}]
        [:div.formic-quill {:class (when @err "error")}
         [:span.formic-input-title
          (u/format-kw id)]
         [:div.formic-quill-editor-wrapper
          [:div 
           {:ref (fn [el] (reset! element el))}]]
         [:input {:type "hidden" :value (prn-str (DEFAULT_SERIALIZER @value))}]
         (when @err
           [:h3.error err])])})))

(field/register-component
 :formic-quill
 {:component quill
  :parser clj->js
  :serializer DEFAULT_SERIALIZER})
