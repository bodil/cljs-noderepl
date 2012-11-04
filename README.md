# cljs-noderepl

Provides a ClojureScript REPL running on Node.JS.

## Installation

Add the following dependency to your `project.clj`:

```clojure
[org.bodil/cljs-noderepl "0.1.0"]
```

## Usage

To launch the REPL the hard way, run `lein repl` and enter the following:

```clojure
(require '[cljs.repl :as repl] '[cljs.repl.node :as node])
(repl/repl (node/repl-env))
```

There really ought to be a Leiningen plugin for this in the future.

## Environment

The REPL is connected to a live Node process running a sandboxed
environment, which provides the `process` and `require` globals.

As an example, here's the standard Node hello world, in REPL ready
ClojureScript:

```clojure
(let [http (js/require "http")
      handler (fn [req res] (.end res "Hello sailor!"))
      server (.createServer http handler)]
  (.listen server 1337))
```

## License

Copyright © 2012 Bodil Stokke.

Distributed under the Eclipse Public License, the same as Clojure.
