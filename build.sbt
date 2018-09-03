organization := "com.github.skjolber"
name := "3d-bin-container-packing"
version := "1.0.10-SNAPSHOT"
description := "3D Bin Container Packing - Library for 3D rectangular bin packing"
licenses += "Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")

scalafmtOnCompile := true

scalaVersion := "2.12.6"

// Jules note:
// -----------
// I remove the `-Xfatal-warnings` from the scalac options set by the `sbt-tpolecat` plugin because it can be frustating.
// However, If you feel confident enough, it's better to activate it.
//
scalacOptions := scalacOptions.value.filter(_ != "-Xfatal-warnings") :+ "-Xlint:unchecked"

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.12" % Test,
  "org.hamcrest" % "hamcrest-core" % "1.3" % Test,
  "org.hamcrest" % "hamcrest-library" % "1.3" % Test,
  "org.hamcrest" % "hamcrest-all" % "1.3" % Test,
  "com.google.truth" % "truth" % "0.42" % Test,
  "org.mockito" % "mockito-core" % "2.21.0" % Test
)

fork in run := true
javaOptions in run ++= Seq(
  "-Xms4G",
  "-Xmx8G",
  "-XshowSettings:vm"
)

pomExtra := (
  <url>https://github.com/skjolber/3d-bin-container-packing</url>
  <scm>
    <connection>scm:git:git@github.com:skjolber/3d-bin-container-packing.git</connection>
    <developerConnection>scm:git:git@github.com:skjolber/3d-bin-container-packing.git</developerConnection>
    <url>git@github.com:skjolber/3d-bin-container-packing.git</url>
    <tag>HEAD</tag>
  </scm>
  <issueManagement>
    <system>GitHub</system>
    <url>https://github.com/skjolber/3d-bin-container-packing/issues</url>
  </issueManagement>
  <ciManagement>
    <system>Travis CI</system>
    <url>https://travis-ci.org/skjolber/3d-bin-container-packing</url>
  </ciManagement>
  <developers>
    <developer>
      <name>Thomas Skjølberg</name>
      <email>thomas.skjolberg@gmail.com</email>
    </developer>
  </developers>
)
