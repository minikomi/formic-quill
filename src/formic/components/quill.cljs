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

(defn DEFAULT_SERIALIZER [v]
  (when (not (string? v)) ;; can be string on initialization
    (js->clj (gobj/get (:delta v) "ops") :keywordize-keys true)))

(def not-blank
  {:message "Required"
   :optional true
   :validate (fn [v] (not-empty (:txt v)))})

(defn quill [{:keys [id touched value err classes options]}]
  (let [{:keys [modules formats theme]} options
        element (r/atom nil)
        editor (r/atom nil)
        should-update (atom false)
        current-handler (atom nil)]
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
          (reset! current-handler
                  (fn [delta olddelta source]
                    (when (= source "user")
                      (swap! value
                             assoc
                             :delta (.getContents @editor)
                             :txt (.getText @editor))
                      (reset! should-update false))))
          (.on ed "text-change" @current-handler)
          ;; ensure touched when blurred
          (set! (.. @element -firstChild -onblur)
                (fn [ev]
                  (reset! touched true)))
          (reset-ed-fn)
          (reset! value {:txt (.getText ed)
                         :delta (.getContents ed)})
          (reset! editor ed)))
      :component-did-update
      (fn [this props]
        (when (and @editor
                   @should-update)
          (.off @editor "text-change" @current-handler)
          (reset! current-handler
                  (fn [delta olddelta source]
                    (when (= source "user")
                      (swap! (:value (r/props this))
                             assoc
                             :delta (.getContents @editor)
                             :txt (.getText @editor))
                      (reset! should-update false))))
          (.on @editor "text-change" @current-handler)
          (let [cv @(:value (r/props this))]
            (if (string? cv)
              (.setText @editor cv)
              (.setContents @editor (:delta cv)))))
        (reset! should-update true))
      :component-will-unmount
      (fn [_]
        (let [ed @editor]
          (.off ed "editor-change")
          (reset! editor nil)))
      :reagent-render
      (fn [{:keys [id touched value err options]}]
        [:div.formic-quill {:class (when @err "error")}
         [:h5.formic-input-title
          {:class (:title classes)}
          (u/format-kw id)]
         [:div.formic-quill-editor-wrapper
          [:div 
           {:ref (fn [el] (reset! element el))}]]
         [:input {:type "hidden" :value (prn-str (DEFAULT_SERIALIZER @value))}]
         (when @err
           [:h3.error @err])])})))

(field/register-component
 :formic-quill
 {:component quill
  :parser clj->js
  :serializer DEFAULT_SERIALIZER})
