import Dependencies._

ThisBuild / scalaVersion     := "2.11.12"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.mdagdelen"
ThisBuild / organizationName := "mdagdelen"

lazy val root = (project in file("."))
  .settings(
    name := "akka-kafka",
    libraryDependencies ++= Seq(
      "ch.qos.logback"        %  "logback-classic"       % "1.2.3",
      "com.typesafe.akka"     %% "akka-stream-kafka"     % "2.0.7"
    ),
    libraryDependencies += scalaTest % Test
  )
