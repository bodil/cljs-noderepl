(ns leiningen.noderepl
  (:use [leinjacker.eval :only [in-project]])
  (:require [leiningen.core.main :as main]
            [leiningen.trampoline :as ltrampoline]
            [leinjacker.deps :as deps]))

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
   (let [project (deps/add-if-missing project '[org.bodil/cljs-noderepl "0.1.11-SNAPSHOT"])]
     (in-project project []
                 (ns (:require [cljs.repl.node :as node]))
                 (node/run-node-repl)))))
