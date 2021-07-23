lazy val akkaHttpVersion = "10.2.4"
lazy val akkaVersion = "2.6.15"
lazy val elastic4sVersion = "7.12.3"

lazy val root = (project in file(".")).
  settings(
    version := "0.1.0",
    dockerExposedPorts ++= Seq(8081),
    dockerBaseImage := "adoptopenjdk/openjdk14:debian",
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.13.4"
    )),
    name := "es-crud",

    libraryDependencies ++= Seq(
      "com.sksamuel.elastic4s" %% "elastic4s-client-sttp" % elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-core" % elastic4sVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
      "com.newmotion" %% "akka-rabbitmq" % "6.0.0",

      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.1.4" % Test
    )
  ).enablePlugins(JavaAppPackaging, DockerPlugin)

