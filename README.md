# cljs-noderepl

Provides a ClojureScript REPL running on Node.JS.

## Requirements

You must have Node version 0.8.x or higher installed.

## Leiningen Plugin

To setup the Leiningen plugin, add this to your `project.clj` or
`~/.lein/profiles.clj`:

```clojure
:plugins [[org.bodil/lein-noderepl "0.1.11"]]
```

Then, start the REPL like this:

```bash
$ lein trampoline noderepl
```

## In-Project Usage

Add the following dependency to your `project.clj`:

```clojure
[org.bodil/cljs-noderepl "0.1.11"]
```

To launch the REPL the hard way, run `lein repl` and enter the following:

```clojure
(require '[cljs.repl :as repl] '[cljs.repl.node :as node])
(repl/repl (node/repl-env))
```

## Environment

The REPL is connected to a live Node process running a sandboxed
environment, which provides all of Node's available global variables.

As an example, here's the standard Node hello world, in REPL ready
ClojureScript:

```clojure
(let [http (js/require "http")
      handler (fn [req res] (.end res "Hello sailor!"))
      server (.createServer http handler)]
  (.listen server 1337))
```

## Readline Support

If you have rlwrap installed, the following provides basic readline and paren matching support:

```bash
rlwrap -r -m -q '\"' -b "(){}[],^%3@\";:'" lein trampoline noderepl
```

## nREPL With Piggieback

You can get cljs-noderepl running on an nREPL server through
[Piggieback](https://github.com/cemerick/piggieback), though it's a
bit fiddly. Here's how.

Add the cljs-noderepl dependency to your `project.clj` (it will bring
in Piggieback transitively):

```clojure
[org.bodil/cljs-noderepl "0.1.11"]
```

You may want to add this dependency to your `:dev` profile so it's not
carried along when you deploy your library/application.

Then, add the Piggieback nREPL middleware, also in `project.clj`:

```clojure
:repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
```

Now, launch nREPL through Leiningen as usual - `lein repl` - and
connect to it from wherever you prefer. You'll need to start the
ClojureScript REPL manually once you have a Clojure REPL prompt:

```clojure
user=> (require '[cljs.repl.node :as node])
user=> (node/run-node-nrepl)
```

## License

Copyright © 2012 Bodil Stokke.

Distributed under the Eclipse Public License, the same as Clojure.
