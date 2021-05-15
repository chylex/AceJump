import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  idea apply true
  kotlin("jvm") version "1.5.0"
  id("org.jetbrains.intellij") version "0.7.2"
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
  }
}

dependencies {
  compileOnly(kotlin("stdlib-jdk8"))
}

repositories {
  mavenCentral()
  jcenter()
}

intellij {
  version = "2021.1"
  pluginName = "AceJump"
  updateSinceUntilBuild = false
  setPlugins("java", "IdeaVIM:0.66")
}

group = "org.acejump"
version = "chylex-7"
