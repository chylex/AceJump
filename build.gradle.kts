import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  idea
  kotlin("jvm") version "1.5.10"
  id("org.jetbrains.intellij") version "1.6.0"
}

repositories {
  mavenCentral()
}

intellij {
  version.set("2022.1.2")
  updateSinceUntilBuild.set(false)
  pluginsRepositories.custom("https://intellij.chylex.com")
  plugins.add("IdeaVIM:chylex-11")
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "11"
}

group = "org.acejump"
version = "chylex-8"
