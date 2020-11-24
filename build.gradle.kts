import org.jetbrains.changelog.closure
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  idea apply true
  kotlin("jvm") version "1.3.72"
  id("org.jetbrains.intellij") version "0.6.5"
  id("org.jetbrains.changelog") version "0.6.2"
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    kotlinOptions.freeCompilerArgs += "-progressive"
  }
  
  withType<PatchPluginXmlTask> {
    sinceBuild("201.6668.0")
    changeNotes({ changelog.getLatest().toHTML() })
  }
}

changelog {
  path = "${project.projectDir}/CHANGES.md"
  header = closure { "${project.version}" }
}

dependencies {
  compileOnly(kotlin("stdlib-jdk8"))
}

repositories {
  mavenCentral()
  jcenter()
}

intellij {
  version = "2020.2"
  pluginName = "AceJump"
  updateSinceUntilBuild = false
  setPlugins("java")
}

idea {
  module {
    excludeDirs.add(buildDir)
  }
}

group = "org.acejump"
version = "4.0"