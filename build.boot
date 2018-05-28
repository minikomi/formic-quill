(def project 'co.poyo/formic-quill)
(def version "0.1.0-SNAPSHOT")

(set-env! :src-paths #{"src/cljs"}
          :dependencies   '[[org.clojure/clojure "1.9.0"]
                            [org.clojure/clojurescript "1.10.238"]
                            [cljsjs/quill "1.2.5-4"]
                            [adzerk/bootlaces "0.1.13"]
                            [reagent "0.8.0"]
                            [funcool/struct "1.2.0"]])

(require '[adzerk.bootlaces :refer :all])

(bootlaces! version)

(task-options!
 pom {:project     project
      :version     version
      :description "Frontend / Backend tools for creating forms declaritively"
      :url         ""
      :scm         {:url ""}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask build
  "Build and install the project locally."
  []
  (comp (pom) (jar) (install)))
