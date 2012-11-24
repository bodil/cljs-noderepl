(ns leiningen.noderepl
  (:use [leinjacker.eval :only [eval-in-project]])
  (:require [leiningen.cljsbuild.config :as config]
            [leiningen.cljsbuild.subproject :as subproject]
            [leiningen.core.main :as main]
            [leiningen.trampoline :as ltrampoline]
            [leinjacker.deps :as deps]))

(defmacro in-project
  ;; Copied from leinjacker HEAD, still missing from 0.4.0
  {:arglists '([project ns-forms* bindings? body*])}
  [project bindings & [form :as forms]]
  (let [bindings (apply hash-map bindings)
        [ns-forms forms] (if (= 'ns (first form))
                           [(rest form) (rest forms)]
                           [nil forms])
        ns-forms (if (symbol? (first ns-forms))
                   ns-forms
                   (cons (gensym) ns-forms))
        f `(fn [[~@(keys bindings)]] ~@forms)]
    `(eval-in-project ~project
                      (list 'do
                            '(ns ~@ns-forms)
                            (list '~f (list 'quote [~@(vals bindings)]))))))

(defn cljsbuild-subproject-fixup [project]
  (assoc project :dependencies (vec (:dependencies project))))

(defn make-subproject [project crossover-path builds]
  (let [subp (cljsbuild-subproject-fixup
              (subproject/make-subproject-lein2 project crossover-path builds))]
    (deps/add-if-missing subp ['org.bodil/cljs-noderepl "0.1.1"])))

(defmacro require-trampoline [& forms]
  `(if ltrampoline/*trampoline?*
     (do ~@forms)
     (do
       (println "You can't run this directly; use \"lein trampoline noderepl\"")
       (main/abort))))

(defn noderepl
  "Launch a ClojureScript REPL on Node.js."
  [project & args]
  (require-trampoline
   (let [{:keys [crossover-path builds]} (config/extract-options project)]
     (in-project (make-subproject project crossover-path builds) []
                 (ns (:require [cljs.repl.node :as node]))
                 (node/run-node-repl)))))
