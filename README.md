Scala Worksheet plugin for Eclipse
==================================

A simple worksheet implementation for Scala IDE. It is a glorified editor, with
the option of evaluating the script and placing the results of each expression in
a comment on the same line.

There are 5 Eclipse plugins:

* the plugin itself
* the `plugin.tests` fragment
* the plugin's runtime library (needed for producing the worksheet's output)
* an Eclipse feature
* an Eclipse source feature
* an Eclipse update-site

The projects can readily be imported inside Eclipse. Additionally, you have maven `pom` files
based on Tycho, enabling command line builds.

## Build

Simply run the ``./build.sh`` script.

The worksheet build is based on
[plugin-profiles](https://github.com/scala-ide/plugin-profiles) and
can be built against several versions of the IDE, Eclipse and Scala
compiler.

Launchin the ``build.sh`` script will exand to the following Maven command:

```
mvn -Peclipse-indigo,scala-2.10.x,scala-ide-stable clean install
```

You can choose a different profile for any of the three axis: eclipse
platform, Scala version and Scala IDE stream. Read [here](https://github.com/scala-ide/scala-worksheet/wiki/Build-the-Worksheet)
for more details.
