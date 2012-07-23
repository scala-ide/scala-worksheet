Scala Worksheet plugin
===================

A simple worksheet implementation for Scala IDE. It is a glorified editor, with
the option of evaluating the script and placing the results of each expression in
a comment on the same line.

There are 5 Eclipse plugins:

* the plugin itself
* the `plugin.tests` fragment
* an Eclipse feature
* an Eclipse source feature
* an Eclipse update-site

The projects can readily be imported inside Eclipse. Additionally, you have maven `pom` files
based on Tycho, enabling command line builds.

## Note:

There are several profiles for choosing what version
of the Scala IDE and Scala compiler you want to build against:

* `scala-ide-2.0-scala-2.9`
* `scala-ide-2.0.x-scala-2.9`
* `scala-ide-master-scala-2.9`
* `scala-ide-master-scala-trunk`

Run maven like this:

    mvn -P scala-ide-master-scala-trunk clean install
