import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "reactivesecurity"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "org.scalaz" %% "scalaz-core" % "7.0.1"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
