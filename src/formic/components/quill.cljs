(ns formic.components.quill
  (:require
   [formic.util :as u]
   [formic.components.inputs :as inputs]
   [reagent.core :as r]
   [reagent.impl.component :refer [extract-props]]
   [cljsjs.quill]
   [formic.field :as field]
   [goog.object :as gobj]
   [clojure.string :as str]))

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
  (if-let [delta (gobj/get (:delta v) "ops")]
    (js->clj delta :keywordize-keys true)
    v))

(def not-blank
  {:message "Required"
   :optional true
   :validate (fn [v]
               (if (:txt v)
                (not (str/blank? (:txt v)))
                (not-empty v)))})


(defn quill [{:keys [id touched value err classes options title]}]
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
                (cond
                  (string? @value) (.setText ed @value)
                  (:delta @value) (.setContents ed (:delta @value))
                  :else (.setContents ed @value)))]
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
      (fn [{:keys [id touched value err options title] :as f}]
        [inputs/common-wrapper
         f
         [:div.formic-quill-editor-wrapper
          [:div.formic-quill-editor-root {:ref (fn [el] (reset! element el))}]
          [:input {:type "hidden" :value (prn-str (DEFAULT_SERIALIZER @value))}]]]
       )})))

(field/register-component
 :formic-quill
 {:component quill
  :parser (fn [v] (clj->js v))
  :serializer DEFAULT_SERIALIZER})
