lazy val CatsEffectVersion = "2.0.0"
lazy val Fs2Version = "2.0.1"
lazy val CirceVersion = "0.12.1"
lazy val log4catsVersion = "1.0.0"
lazy val ScalacacheVersion = "0.27.0"
lazy val ScalaOpentracingVersion = "0.1.0"
lazy val PureConfigVersion = "0.12.1"

enablePlugins(JmhPlugin)

lazy val sharedSettings = Seq(
  organization := "notthatbreezy",
  name := "memcached4s",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.12.10",
  scalacOptions := Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-Ypartial-unification"
  ),
  resolvers ++= Seq(
    "Geotoolkit Repo" at "http://maven.geotoolkit.org",
    "Open Source Geospatial Foundation Repo" at "http://download.osgeo.org/webdav/geotools/",
    "boundless" at "https://repo.boundlessgeo.com/main/",
    "imageio-ext Repository" at "http://maven.geo-solutions.it",
    Resolver.bintrayRepo("azavea", "geotrellis"),
    "locationtech-releases" at "https://repo.locationtech.org/content/groups/releases",
    "locationtech-snapshots" at "https://repo.locationtech.org/content/groups/snapshots",
    Resolver.bintrayRepo("colisweb", "maven")
  )
)

lazy val root = project
  .in(file("."))
  .settings(sharedSettings: _*)
  .aggregate(client)

lazy val client = project
  .in(file("client"))
  .settings(sharedSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % CatsEffectVersion,
      "co.fs2" %% "fs2-core" % Fs2Version,
      "co.fs2" %% "fs2-io" % Fs2Version,
      "io.circe" %% "circe-core" % CirceVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "io.chrisdavenport" %% "log4cats-slf4j" % log4catsVersion,
      "com.colisweb" %% "scala-opentracing" % ScalaOpentracingVersion
    )
  )

lazy val benchmark = project
  .in(file("benchmark"))
  .dependsOn(client)
  .enablePlugins(JmhPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.cb372" %% "scalacache-core" % ScalacacheVersion,
      "com.github.cb372" %% "scalacache-memcached" % ScalacacheVersion,
      "com.github.cb372" %% "scalacache-circe" % ScalacacheVersion
    )
  )

lazy val example = project
  .in(file("example"))
  .dependsOn(client)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.pureconfig" %% "pureconfig" % PureConfigVersion
    )
  )
