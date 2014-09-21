/*global global:true, process:true, require:true */

var vm = require("vm");

var buildContext = function() {
  var contextProperties = [
    "ArrayBuffer", "Buffer", "DataView",
    "Float32Array", "Float64Array",  "Infinity",
    "Int16Array", "Int32Array", "Int8Array", "NaN",
    "Uint16Array", "Uint32Array", "Uint8Array",
    "Uint8ClampedArray", "clearInterval", "clearTimeout", "console", "process",
    "require", "setInterval", "setTimeout"
  ];
  var i, key, context = {};
  for (i = 0; i < contextProperties.length; i++) {
    key = contextProperties[i];
    if (global.hasOwnProperty(key))
      context[key] = global[key];
  }
  context.global = context;
  return context;
};

var context = vm.createContext(buildContext());
var buffer = "";

process.stdout._r_write = process.stdout.write;

var write = function(data) {
  process.stdout._r_write(data);
};

process.stdout.write = function(out) {
  write(JSON.stringify({ output: out, channel: "stdout" }) + "\n");
};

process.stderr.write = function(out) {
  write(JSON.stringify({ output: out, channel: "stderr" }) + "\n");
};

var pop = function() {
  var i = buffer.indexOf("\n"), l;
  if (i < 0) return null;
  i += 1;
  l = buffer.slice(0, i);
  buffer = buffer.slice(i);
  return l;
};

var to_str = function (x)
{
  try {
    return ((void 0 !== x) && x.toString) ? x.toString () : x;
  } catch (e) {
    return "";
  }
}

process.stdin.on("data", function(sexp) {
  var result, data;

  buffer = buffer + sexp;
  while ((data = pop())) {
    try {
      data = JSON.parse(data);
    } catch (e) {
      write(JSON.stringify({
        error: { name: "CljParseError", message: "Can't parse input" }
      }) + "\n");
      continue;
    }
    buffer = "";
    if (!data.hasOwnProperty("code") || typeof data.code !== "string" || !data.hasOwnProperty("file") || typeof data.file !== "string") {
      write(JSON.stringify("Input must be of the form '{:file \"<filename>\" :code \"<code>\"}'") + "\n");
      continue;
    }
    try {
      result = { result: to_str (vm.runInContext(data.code, context, data.file)) };
    } catch (e) {
      result = { error: { name: e.name, message: e.message, stack: e.stack } };
    }
    write(JSON.stringify(result) + "\n");
  }
});

process.stdin.on("end", function() {
  process.exit(0);
});

process.stdin.resume();
