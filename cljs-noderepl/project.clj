(defproject org.bodil/cljs-noderepl "0.1.9"
  :description "Node.js REPL environment for Clojurescript"
  :url "https://github.com/bodil/cljs-noderepl"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1586"]
                 [cheshire "5.2.0"]
                 [com.cemerick/piggieback "0.0.5"]]
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]})
