import play.Play.autoImport._
import sbt._
import Keys._
import PlayKeys._
import play.Play.autoImport._

object ApplicationBuild extends Build {

  val appName         = "reactivesecurity"
  val appVersion      = "1.4.1-SNAPSHOT"

  val appDependencies = Seq(
    ws,
    cache,
    "org.scalaz" %% "scalaz-core" % "7.1.0",
    "com.softwaremill.macwire" %% "macros" % "1.0.5",
    "com.softwaremill.macwire" %% "runtime" % "1.0.5",
    "org.mindrot" % "jbcrypt" % "0.3m"
  )

  val main = Project(appName, file(".")).enablePlugins(play.sbt.Play).settings(
    scalaVersion := "2.11.7",
    version := appVersion,
    resolvers ++= Seq(
      "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
    ),
    libraryDependencies ++= appDependencies,
    publishTo := Some(Resolver.file(
      "Github Pages", new File("/home/nick/publish/reactivesecurity"))
    ) 
  )
}
