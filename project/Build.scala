import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.SbtGit.git
import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.packager.archetypes._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.sbtplugin.cross.CrossType
import sbt.Keys._
import sbt._
import sbtbuildinfo._
import sbtbuildinfo.BuildInfoKeys._
import spray.revolver.RevolverPlugin.autoImport._



/**
 * Application settings. Configure the build for your application here.
 * You normally don't have to touch the actual build definition after this.
 */
object Settings {
  /** The name of your application */
  val name = "isomorphic"


  /** Options for the scala compiler */
  val scalacOptions = Seq(
    "-Xlint",
    "-unchecked",
    "-deprecation",
    "-feature",
//    "-Ymacro-debug-lite", //only uncomment when you really need to debug a macro
    "-language:postfixOps"
  )

  /** Set some basic options when running the project with Revolver */
  val jvmRuntimeOptions = Seq(
    "-Xmx1G"
  )

  /** Declare global dependency versions here to avoid mismatches in multi part dependencies */
  object versions {
    val scala = "2.11.8"
    val akka = "2.4.10"

    val scalajsReact = "0.11.1"
    val react = "15.3.1"
    val scalajsDom = "0.9.1"
    val scalaTags = "0.6.0"
  }

  /**
   * These dependencies are shared between JS and JVM projects
   * the special %%% function selects the correct version for each project
   */
  val sharedDependencies = Def.setting(Seq(
  ))

  /** Dependencies only used by the JVM project */
  val jvmDependencies = Def.setting(Seq(
    "com.typesafe.akka" %% "akka-actor" % versions.akka,
    "com.typesafe.akka" %% "akka-stream" % versions.akka,
    "com.typesafe.akka" %% "akka-http-core" % versions.akka,
    "com.typesafe.akka" %% "akka-http-experimental" % versions.akka,
    "com.lihaoyi" %% "scalatags" % versions.scalaTags
  ))

  /** Dependencies only used by the JS project (note the use of %%% instead of %%) */
  val scalajsDependencies = Def.setting(Seq(
    "com.github.japgolly.scalajs-react" %%% "core" % versions.scalajsReact
    // "com.github.japgolly.scalajs-react" %%% "extra" % versions.scalajsReact,
    // "org.scala-js" %%% "scalajs-dom" % versions.scalajsDom
  ))

  /** Dependencies for external JS libs that are bundled into a single .js file according to dependency order */
  val jsDependencies = Def.setting(Seq(
    "org.webjars.bower" % "react" % versions.react / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
    // "org.webjars.bower" % "react" % versions.react / "react.js" minified "react.min.js" commonJSName "React",
    "org.webjars.bower" % "react" % versions.react / "react-dom.js"         minified "react-dom.min.js" dependsOn "react-with-addons.js" commonJSName "ReactDOM",
    "org.webjars.bower" % "react" % versions.react / "react-dom-server.js"  minified "react-dom-server.min.js" dependsOn "react-dom.js" commonJSName "ReactDOMServer"
  ))

  /** Same dependecies, but for production build, using minified versions */
  val jsDependenciesProduction = Def.setting(Seq(
    "org.webjars.bower" % "react" % versions.react / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
    // "org.webjars.bower" % "react" % versions.react / "react.js" minified "react.min.js" commonJSName "React",
    "org.webjars.bower" % "react" % versions.react / "react-dom.js"         minified "react-dom.min.js" dependsOn "react-with-addons.js" commonJSName "ReactDOM",
    "org.webjars.bower" % "react" % versions.react / "react-dom-server.js"  minified "react-dom-server.min.js" dependsOn "react-dom.js" commonJSName "ReactDOMServer"
  ))
}


object ApplicationBuild extends Build {

  val globalBuildInfoKeys = Seq[BuildInfoKey](
      name, version, scalaVersion, sbtVersion,
      "scalaJSVersion" -> scalaJSVersion.toString)
  val globalBuildInfoOptions = Seq[BuildInfoOption](BuildInfoOption.ToMap, BuildInfoOption.BuildTime)

  // ripped from http://blog.byjean.eu/2015/07/10/painless-release-with-sbt.html
  val versionRegex = "v([0-9]+.[0-9]+.[0-9]+)-?(.*)?".r
  git.gitTagToVersionNumber := {
    case versionRegex(v,"") => Some(v)
    case versionRegex(v,"SNAPSHOT") => Some(s"$v-SNAPSHOT")
    case versionRegex(v,s) => Some(s"$v-$s-SNAPSHOT")
    case _ => None
  }


  // root project aggregating the JS and JVM projects
  lazy val root = project.in(file(".")).
    aggregate(js, jvm).
    settings(
      name := Settings.name,
      scalaVersion := Settings.versions.scala,
      publish := {},
      publishLocal := {},
      updateOptions := updateOptions.value.withCachedResolution(true)
    )

  val sharedSrcDir = "shared"

  val productionBuild = settingKey[Boolean]("Build for production")
  val elideOptions = settingKey[Seq[String]]("Set limit for elidable functions")
  val copyWebJarResources = taskKey[Unit]("Copy resources from WebJars")


  // a special crossProject for configuring a JS/JVM/shared structure
  lazy val sharedProject = crossProject.in(file("."))
    .settings(
      name := Settings.name,
      scalaVersion := Settings.versions.scala,
      pollInterval := 5000,
      scalacOptions ++= Settings.scalacOptions,
      libraryDependencies ++= Settings.sharedDependencies.value,

      copyWebJarResources := {
        // copy the compiled CSS
        val s = streams.value
        s.log("Copying webjar resources")
        val targetDir = (classDirectory in Compile).value / "web"
        IO.createDirectory(targetDir / "stylesheets")
      },

      // run the copy after compile/assets but before managed resources
      copyWebJarResources <<= copyWebJarResources dependsOn(compile in Compile),
      managedResources in Compile <<= (managedResources in Compile) dependsOn copyWebJarResources,
      updateOptions := updateOptions.value.withCachedResolution(true)
    )

    // set up settings specific to the JVM project
    .jvmSettings(Revolver.settings: _*)
    .jvmSettings(
      libraryDependencies ++= Settings.jvmDependencies.value,

      // copy resources from the "shared" project
      unmanagedResourceDirectories in Compile += file(".") / sharedSrcDir / "src" / "main" / "resources",
      unmanagedResourceDirectories in Test += file(".") / sharedSrcDir / "src" / "test" / "resources",

      // javaOptions in Revolver.reStart ++= Settings.jvmRuntimeOptions,
      javaOptions in reStart ++= Settings.jvmRuntimeOptions,
      mainClass in reStart := Some("com.github.matthewpflueger.isomorphic.server.Main"),

      // configure a specific port for debugging, so you can easily debug multiple projects at the same time if necessary
      Revolver.enableDebugging(port = 5111, suspend = false),

      resolvers += Resolver.mavenLocal
    )

    // set up settings specific to the JS project
    .jsSettings(
      libraryDependencies ++= Settings.scalajsDependencies.value,
      // by default we do development build
      productionBuild := false,
      elideOptions := Seq(),
      scalacOptions ++= elideOptions.value,
      // scalacOptions in (Compile, fullOptJS) ++= Seq("-Xelide-below", "WARNING"),
      // select JS dependencies according to build setting
      jsDependencies ++= { if (!productionBuild.value) Settings.jsDependencies.value else Settings.jsDependenciesProduction.value },
      scalacOptions ++= Seq({
        val a = js.base.toURI.toString.replaceFirst("[^/]+/?$", "")
        s"-P:scalajs:mapSourceURI:$a->/srcmaps/"
      }),


      // yes, we want to package JS dependencies
      skip in packageJSDependencies := false,

      // copy resources from the "shared" project
      unmanagedResourceDirectories in Compile += file(".") / sharedSrcDir / "src" / "main" / "resources",
      unmanagedResourceDirectories in Test += file(".") / sharedSrcDir / "src" / "test" / "resources"
    )

  // configure a specific directory for scalajs output
  val scalajsOutputDir = Def.settingKey[File]("directory for javascript files output by scalajs")

  // make all JS builds use the output dir defined later
  lazy val js2jvmSettings = Seq(fastOptJS, fullOptJS, packageJSDependencies) map { packageJSKey =>
    crossTarget in(js, Compile, packageJSKey) := scalajsOutputDir.value
  }

  // instantiate the JS project for SBT with some additional settings
  lazy val js: Project = sharedProject.js.settings(
    fastOptJS in Compile := {
      // make a copy of the produced JS-file (and source maps) under the js project as well,
      // because the original goes under the jvm project
      // NOTE: this is only done for fastOptJS, not for fullOptJS
      val base = (fastOptJS in Compile).value
      IO.copyFile(base.data, (classDirectory in Compile).value / "web" / "js" / base.data.getName)
      IO.copyFile(base.data, (classDirectory in Compile).value / "web" / "js" / (base.data.getName + ".map"))
      base
    },

    packageJSDependencies in Compile := {
      // make a copy of the produced jsdeps file under the js project as well,
      // because the original goes under the jvm project
      val base = (packageJSDependencies in Compile).value
      IO.copyFile(base, (classDirectory in Compile).value / "web" / "js" / base.getName)
      base
    },

    buildInfoKeys := globalBuildInfoKeys,
    buildInfoOptions := globalBuildInfoOptions,
    buildInfoPackage := "com.github.matthewpflueger.isomorphic.client"
  ).enablePlugins(BuildInfoPlugin, GitVersioning)


  // instantiate the JVM project for SBT with some additional settings
  lazy val jvm: Project = sharedProject.jvm.settings(js2jvmSettings: _*).settings(
    // scala.js output is directed under "web/js" dir in the jvm project
    scalajsOutputDir := (classDirectory in Compile).value / "web" / "js",
    mainClass in Compile := Some("com.github.matthewpflueger.isomorphic.server.Main"),
    // set environment variables in the execute scripts
    NativePackagerKeys.bashScriptExtraDefines += """addJava "-Djava.net.preferIPv4Stack=true" """,
    NativePackagerKeys.bashScriptExtraDefines += """addJava "-Xms1024m" """,
    NativePackagerKeys.bashScriptExtraDefines += """addJava "-Xmx2048m" """,
    // reStart depends on running fastOptJS on the JS project
//    Revolver.reStart <<= Revolver.reStart dependsOn (fastOptJS in(js, Compile))

    buildInfoKeys := globalBuildInfoKeys,
    buildInfoOptions := globalBuildInfoOptions,
    buildInfoPackage := "com.github.matthewpflueger.isomorphic.server"
  ).enablePlugins(BuildInfoPlugin, GitVersioning, JavaAppPackaging)

}