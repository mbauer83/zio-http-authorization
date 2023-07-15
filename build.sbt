import BuildHelper._

inThisBuild(
  List(
    organization             := "dev.michaelbauer",
    licenses                 := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers               := List(
      Developer("mbauer", "Michael Bauer", "mbauer.mphil@googlemail.com", url("https://github.com/mbauer83")),
    ),
    Test / fork              := true,
    Test / parallelExecution := false,
  ),
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
addCommandAlias("fix", "; all compile:scalafix test:scalafix; all scalafmtSbt scalafmtAll")

val zioVersion     = "2.0.15"
val zioHTTPVersion = "3.0.0-RC2"

lazy val root =
  project.in(file(".")).settings(publish / skip := true).aggregate(zio_rbac, docs)

lazy val zio_rbac = project
  .in(file("zio-rbac"))
  .settings(stdSettings("zio-rbac"))
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"          % zioVersion,
      "dev.zio" %% "zio-streams"  % zioVersion,
      "dev.zio" %% "zio-http"     % zioHTTPVersion,

      "org.scalameta"      %% "munit"        % "0.7.29"     % Test,
      "com.github.poslegm" %% "munit-zio"    % "0.1.1"      % Test,
      "dev.zio"            %% "zio-mock"     % "1.0.0-RC10" % Test,
      "dev.zio"            %% "zio-test"     % zioVersion   % Test,
      "dev.zio"            %% "zio-test-sbt" % zioVersion   % Test,
    ),
  )

lazy val docs = project
  .in(file("zio-rbac-docs"))
  .settings(
    moduleName                                 := "zio-rbac-docs",
    scalacOptions += "-groups",
    scalacOptions -= "-Yno-imports",
    scalacOptions -= "-Xfatal-warnings",
    libraryDependencies ++= Seq("dev.zio" %% "zio" % zioVersion),
    scalaVersion                               := Scala3,
    crossScalaVersions                         := Seq(Scala213, Scala3),
    projectName                                := "ZIO REST RBAC",
    mainModuleName                             := (zio_rbac / moduleName).value,
    projectStage                               := ProjectStage.Development,
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(zio_rbac),
    docsPublishBranch                          := "series/0.x",
  )
  .dependsOn(zio_rbac)
  .enablePlugins(WebsitePlugin)
