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
  "Launch a ClojureScript REPL on Node.js.

  Usage:

    lein trampoline noderepl
    lein trampoline noderepl :node-command /path/to/node"
  [project & {:as opts}]
  (require-trampoline
   (let [project (deps/add-if-missing project '[org.bodil/cljs-noderepl "0.1.11"])]
     (in-project project [node-command (if opts (opts ":node-command") "node")]
                 (ns (:require [cljs.repl.node :as node]
                               [cljs.repl :as repl]))
                 (repl/repl (node/repl-env :node-command node-command))))))
