ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "AkkaHttpServer"
  )
//

resolvers += "Akka library repository".at("https://repo.akka.io/maven")
val AkkaVersion = "2.9.0"
val AkkaHttpVersion = "10.6.0"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
)

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-http-spray-json
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.0"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.6.0"
