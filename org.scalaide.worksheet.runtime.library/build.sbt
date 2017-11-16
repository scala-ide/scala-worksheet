name := "org.scalaide.worksheet.runtime.library"
version := "0.8.0-SNAPSHOT"
scalaVersion := "2.12.3"
moduleName := name.value
organization := "org.scalaide"
crossScalaVersions := Seq("2.10.6", "2.11.11", "2.12.3")
crossVersion := CrossVersion.full
fork := true
parallelExecution in Test := false
autoAPIMappings := true

scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, 11)) => Seq(
    "-deprecation:false",
    "-encoding", "UTF-8",
    "-feature",
    "-language:_",
    "-unchecked",
    "-Xlint",
    "-Xfuture",
    "-Xfatal-warnings",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-unused-import",
    "-Ywarn-unused"
  )
  case Some((2, 12)) => Seq(
    "-deprecation:false",
    "-encoding", "UTF-8",
    "-feature",
    "-language:_",
    "-unchecked",
    "-Xlint:-unused,_",
    "-Xfuture",
    "-Xfatal-warnings",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-unused:imports,privates,locals,-patvars,-params,-implicits,_"
  )
  case _ => Seq()
})

publishMavenStyle := true

publishTo := {
  val repo = sys.props.get("maven.repo.local").orElse {
    sys.props.get("user.home").flatMap { home =>
      sys.props.get("file.separator").map(home + _ + ".m2")
    }
  }
  repo.map("local-repo" at "file://" + _)
}
