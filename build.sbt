val scala3Version = "3.1.1"

lazy val munitVersion = "0.7.29"

lazy val nettyVersion = "4.1.73.Final"

lazy val root = project
  .in(file("."))
  .settings(
    name := "armeria-tutorial-blog-service",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "com.linecorp.armeria" %% "armeria-scala" % "1.14.0",
      "ch.qos.logback" % "logback-classic" % "1.2.10",
      "com.novocode" % "junit-interface" % "0.11" % "test"
    )
  )
