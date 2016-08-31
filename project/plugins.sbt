addSbtPlugin("com.trueaccord.scalapb" % "sbt-scalapb" % "0.5.34")

// Protoc-jar so we don't need the Protoc compiler
libraryDependencies += "com.github.os72" % "protoc-jar" % "3.0.0-b3"