name := "kafka-security-manager"

organization := "com.github.simplesteph.ksm"

version := "0.8"

scalaVersion := "2.12.8"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .enablePlugins(ClasspathJarPlugin)


resolvers ++= Seq(
  "Artima Maven Repository" at "https://repo.artima.com/releases",
  Resolver.bintrayRepo("beyondthelines", "maven")
)

libraryDependencies ++= Seq(
  // kafka
  "org.apache.kafka" %% "kafka" % "2.3.1",
  "io.github.embeddedkafka" %% "embedded-kafka" % "2.3.1" % "test",

  "org.apache.kafka" % "kafka-clients" % "2.3.1", // needed explicitly for proper classPath
  "org.apache.kafka" % "kafka-clients" % "2.3.1" % Test classifier "test",

  // test
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,

  // logging
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "org.slf4j" % "slf4j-log4j12" % "1.7.25",

  // config
  "com.typesafe" % "config" % "1.3.3",

  // parsers
  "com.github.tototoshi" %% "scala-csv" % "1.3.5",

  // APIs
  "org.skinny-framework" %% "skinny-http-client" % "2.3.7",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.9.4",

  // AWS SDK to access S3
  "software.amazon.awssdk" % "s3" % "2.29.36",
  // STS for OIDC federation support in IAM
  "software.amazon.awssdk" % "sts" % "2.29.36"

)

Compile / mainClass := Some("com.github.simplesteph.ksm.KafkaSecurityManager")

Test / parallelExecution := false

// Docker stuff
dockerRepository := Some("simplesteph")
dockerUpdateLatest := true
dockerBaseImage := "openjdk:8-jre-slim"

// Add the default sonatype repository setting
publishTo := sonatypePublishTo.value
