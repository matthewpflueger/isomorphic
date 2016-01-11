// repository for Typesafe plugins
resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

// explicitly add Maven Central in order to resolve workbench plugin below
resolvers += "Maven central" at "https://repo1.maven.org/maven2/"

addSbtPlugin("com.hanhuy.sbt" % "android-sdk-plugin" % "1.5.13")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.1")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.5")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.5.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")

//addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.3")
