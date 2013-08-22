import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "Sample"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "reactivesecurity" %% "reactivesecurity" % "1.0-SNAPSHOT"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    scalaVersion := 2.10.2
  )

}
