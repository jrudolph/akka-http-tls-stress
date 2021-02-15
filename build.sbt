name := "akka-http-tls-stress"

version := "0.1"

scalaVersion := "2.13.4"

val akkaHttpVersion = "10.2.3"
val akkaVersion = "2.6.12"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)
