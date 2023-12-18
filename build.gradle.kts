@file:Suppress("ConvertLambdaToReference")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.9.10"
  id("org.jetbrains.intellij") version "1.16.1"
}

group = "org.acejump"
version = "chylex-18"

repositories {
  mavenCentral()
}

intellij {
  version.set("2023.3")
  updateSinceUntilBuild.set(false)
  plugins.add("IdeaVIM:chylex-22")
  
  pluginsRepositories {
    custom("https://intellij.chylex.com")
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

tasks.patchPluginXml {
  sinceBuild.set("233")
}

tasks.buildSearchableOptions {
  enabled = false
}

tasks.test {
  useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
  kotlinOptions.freeCompilerArgs = listOf(
    "-Xjvm-default=all"
  )
}
