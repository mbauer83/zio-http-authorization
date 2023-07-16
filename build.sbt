import BuildHelper._

inThisBuild(
  List(
    organization             := "dev.michaelbauer",
    licenses                 := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers               := List(
      Developer("mbauer", "Michael Bauer", "mbauer.mphil@googlemail.com", url("https://github.com/mbauer83")),
    ),
    scmInfo       := Some(
      ScmInfo(url("https://github.com/mbauer83/zio-http-authorization"), "scm:git:git@github.com:mbauer83/zio-http-authorization.git")
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
  project.in(file(".")).settings(publish / skip := true).aggregate(zio_http_authorization, docs)

lazy val zio_http_authorization = project
  .in(file("zio-http-authorization"))
  .settings(stdSettings("zio-http-authorization"))
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
  .in(file("zio-http-authorization-docs"))
  .settings(
    moduleName                                  := "zio-http-authorization-docs",
    scalacOptions                               -= "-Yno-imports",
    scalacOptions                               -= "-Xfatal-warnings",
    libraryDependencies                         ++= Seq("dev.zio" %% "zio" % zioVersion),
    scalaVersion                                := Scala3,
    crossScalaVersions                          := Seq(Scala213, Scala3),
    projectName                                 := "Zio-http-authorization",
    mainModuleName                              := (zio_http_authorization / moduleName).value,
    projectStage                                := ProjectStage.Development,
    ScalaUnidoc / unidoc / unidocProjectFilter  := inProjects(zio_http_authorization),
    siteSourceDirectory                         := target.value / "api",
    previewFixedPort                            := Some(8888),
    previewPath                                 := "index.html",
    makeSite / mappings                         ++= Seq(
                                                      file("LICENSE") -> "LICENSE",
                                                    ),
    git.remoteRepo                              := "git@github.com:mbauer83/zio-http-authorization.git",
  )
  .dependsOn(zio_http_authorization)
  .enablePlugins(WebsitePlugin, SiteScaladocPlugin, GhpagesPlugin)
