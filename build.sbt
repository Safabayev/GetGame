import Dependencies.*
import sbt.Keys.scalacOptions

name := "get_ame_ez"

lazy val root = project
  .in(file("."))
  .settings(
    scalacOptions ++= CompilerOptions.scalacOptions,
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.13.2" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
  )
  .settings(
    publishMavenStyle                      := true,
    version                                := "1.0",
    scalaVersion                           := "2.13.10",
    Compile / doc / sources                := Seq.empty,
    Compile / packageDoc / publishArtifact := false,
  )
  .settings(
    libraryDependencies ++=
      Dependencies.io.circe.all ++
        Dependencies.io.getquill.all ++
        org.http4s.all ++
        org.sangria.all ++
        com.github.pureconfig.all ++
        com.softwaremill.sttp.all ++
        Seq(
          org.typelevel.cats.core,
          org.flywaydb.core,
          org.typelevel.cats.effect,
          org.postgresql,
          ch.qos.logback,
        )
  )

Global / lintUnusedKeysOnLoad := false
Global / onChangedBuildSource := ReloadOnSourceChanges
