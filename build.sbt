name := "torqueweb"

version := "1.0-SNAPSHOT"

resolvers += "mandubian maven bintray" at "http://dl.bintray.com/mandubian/maven"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
   "com.mandubian"     %% "play-json-zipper"    % "1.1",
   "org.seleniumhq.selenium" % "selenium-chrome-driver" % "2.32.0",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2"
)     
 
play.Project.playScalaSettings
