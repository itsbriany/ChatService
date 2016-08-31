import com.trueaccord.scalapb.{ScalaPbPlugin => PB}

name := """ChatService"""

version := "1.0"

scalaVersion := "2.11.8"

val akkaVersion = "2.4.9"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,

  // allows ScalaPB proto customizations (scalapb/scalapb.proto)
  "com.trueaccord.scalapb" %% "scalapb-runtime" % "0.5.34" % PB.protobufConfig
)


PB.protobufSettings ++ Seq(
  scalaSource in PB.protobufConfig <<= (sourceDirectory in Compile) (_ / "generated")
)
PB.runProtoc in PB.protobufConfig := {
  args => com.github.os72.protocjar.Protoc.runProtoc("-v300" +: args.toArray)
}
version in PB.protobufConfig := "3.0.0-beta-3"
