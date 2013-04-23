Scala Worksheet plugin
===================

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

The worksheet build is based on
[plugin-profiles](https://github.com/scala-ide/plugin-profiles) and
can be built against several versions of the IDE, Eclipse and Scala
compiler. For example:

```
mvn -Peclipse-indigo,scala-2.10.x,scala-ide-stable clean install
```

You can choose a different profile for any of the three axis: eclipse
platform, Scala version and Scala IDE stream. Read [here](https://github.com/scala-ide/scala-worksheet/wiki/Build-the-Worksheet)
for more details.
