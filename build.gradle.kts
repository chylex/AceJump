@file:Suppress("ConvertLambdaToReference")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.9.10"
  id("org.jetbrains.intellij") version "1.17.3"
}

group = "org.acejump"
version = "chylex-23"

repositories {
  mavenCentral()
}

intellij {
  version.set("2024.2")
  updateSinceUntilBuild.set(false)
  
  plugins.add("IdeaVIM:chylex-40")
  plugins.add("com.intellij.classic.ui:242.20224.159")
  
  pluginsRepositories {
    custom("https://intellij.chylex.com")
    marketplace()
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

tasks.patchPluginXml {
  sinceBuild.set("242")
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
