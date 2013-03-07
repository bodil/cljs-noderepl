(ns cljs.repl.node
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [cljs.analyzer :as ana]
            [cljs.repl :as repl]
            [cheshire.core :refer [parse-string generate-string]]
            [cemerick.piggieback :as piggieback])
  (:import cljs.repl.IJavaScriptEnv
           java.io.PipedReader
           java.io.PipedWriter))

(defn- load-as-tempfile
  "Copy a file from the classpath into a temporary file.
  Return the path to the temporary file."
  [filename]
  (let [tempfile (java.io.File/createTempFile "cljsrepl" ".js")
        resource (io/resource filename)]
    (.deleteOnExit tempfile)
    (assert resource (str "Can't find " filename " in classpath"))
    (with-open [in (io/input-stream resource)
                out (io/output-stream tempfile)]
      (io/copy in out))
    (.getAbsolutePath tempfile)))

(defn- output-filter
  "Take a reader and wrap a filter around it which swallows and
  acts on output events from the subprocess. Keep the filter
  thread running until alive-func returns false."
  [reader alive-func]
  (let [pipe (PipedWriter.)]
    (future
      (while (alive-func)
        (let [line (.readLine reader)
              data (parse-string line)]
          (if-let [output (get data "output")]
            (print output)
            (doto pipe
              (.write (str line "\n"))
              (.flush))))))
    (io/reader (PipedReader. pipe))))

(defn- process-alive?
  "Test if a process is still running."
  [^Process process]
  (try (.exitValue process) false
       (catch IllegalThreadStateException e true)))

(defn- launch-node-process
  "Launch the Node subprocess."
  []
  ;; Launch repl.js through an eval to trick Node into thinking it was
  ;; started from the current directory, allowing require() to work as
  ;; expected.
  (let [launch-script
        (str "eval(require('fs').readFileSync('"
             (string/replace (load-as-tempfile "cljs/repl/node_repl.js") "\\" "/")
             "','utf8'))")
        process (let [pb (ProcessBuilder. ["node" "-e" launch-script])]
                  (.start pb))]
    {:process process
     :input (output-filter (io/reader (.getInputStream process)) #(process-alive? process))
     :output (io/writer (.getOutputStream process))
     :loaded-libs (atom #{})}))

(defn js-eval [env filename line code]
  (let [{:keys [input output]} env]
    (.write output (str (generate-string {:file filename :line line :code code})
                        "\n"))
    (.flush output)
    (let [result-string (.readLine input)]
      (parse-string result-string true))))

(defn node-setup [repl-env]
  (let [env (ana/empty-env)]
    (repl/load-file repl-env "cljs/core.cljs")
    (swap! (:loaded-libs repl-env) conj "cljs.core")
    (repl/evaluate-form repl-env env "<cljs repl>"
                        '(ns cljs.user))
    (repl/evaluate-form repl-env env "<cljs repl>"
                        '(set! cljs.core/*print-fn* (.-print (js/require "util"))))))

(defn node-eval [repl-env filename line js]
  (let [result (js-eval repl-env filename line js)]
    (if-let [error (:error result)]
      {:status :exception :value (:stack error)}
      {:status :success :value (:result result)})))

(defn load-javascript [repl-env ns url]
  (let [missing (remove #(contains? @(:loaded-libs repl-env) %) ns)]
    (when (seq missing)
      (js-eval repl-env (.toString url) 1 (slurp url))
      (swap! (:loaded-libs repl-env) (partial apply conj) missing))))

(defn node-tear-down [repl-env]
  (let [process (:process repl-env)]
    (doto process
      (.destroy)
      (.waitFor))))

(defn load-resource
  "Load a JS file from the classpath into the REPL environment."
  [env filename]
  (let [resource (io/resource filename)]
    (assert resource (str "Can't find " filename " in classpath"))
    (js-eval env filename 1 (slurp resource))))

(defrecord NodeEnv []
  repl/IJavaScriptEnv
  (-setup [this]
    (node-setup this))
  (-evaluate [this filename line js]
    (node-eval this filename line js))
  (-load [this ns url]
    (load-javascript this ns url))
  (-tear-down [this]
    (node-tear-down this)))

(defn repl-env
  "Create a Node.js REPL environment."
  [& {:as opts}]
  (let [base (io/resource "goog/base.js")
        deps (io/resource "goog/deps.js")
        process (launch-node-process)
        new-repl-env (merge (NodeEnv.)
                            (merge process
                                   {:optimizations :simple}))]
    (assert base "Can't find goog/base.js in classpath")
    (assert deps "Can't find goog/deps.js in classpath")
    (load-resource new-repl-env "goog/base.js")
    (load-resource new-repl-env "goog/deps.js")
    new-repl-env))

(defn run-node-repl []
  (repl/repl (repl-env)))

(defn nrepl-env []
  (doto (repl-env) (node-setup)))

(defn run-node-nrepl []
  (piggieback/cljs-repl :repl-env (nrepl-env)))
