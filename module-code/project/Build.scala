import play.Play.autoImport._
import sbt._
import Keys._
import PlayKeys._

object ApplicationBuild extends Build {

  val appName         = "reactivesecurity"
  val appVersion      = "1.3.0-SNAPSHOT"

  val appDependencies = Seq(
    cache,
    ws,
    "org.scalaz" %% "scalaz-core" % "7.1.0",
    "org.mindrot" % "jbcrypt" % "0.3m"
  )

  val main = Project(appName, file(".")).enablePlugins(play.PlayScala).settings(
    scalaVersion := "2.11.1",
    version := appVersion,
    resolvers ++= Seq(
      "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
    ),
    libraryDependencies ++= appDependencies,
    publishTo := Some(Resolver.file(
      "Github Pages", new File("/home/jobhive/publish/reactivesecurity"))
    ) 
  )
}
