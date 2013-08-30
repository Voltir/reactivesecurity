import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "reactivesecurity"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    cache,
    "org.scalaz" %% "scalaz-core" % "7.0.1",
    "org.mindrot" % "jbcrypt" % "0.3m"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers ++= Seq(
      "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
    ),
    publishTo := Some(Resolver.file(
      "Github Pages", new File("/home/jobhive/publish/reactivesecurity"))
    ) 
  )

}
