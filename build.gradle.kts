import org.jetbrains.gradle.ext.GradleTask
import org.jetbrains.gradle.ext.ProjectSettings
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

tasks {
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    kotlinOptions.freeCompilerArgs += "-progressive"
  }

  named("buildPlugin") { dependsOn("test") }

  withType<RunIdeTask> {
    dependsOn("test")
    findProperty("luginDev")?.let { args = listOf(projectDir.absolutePath) }
  }

  withType<Zip> {
    archiveFileName.set("AceJump.zip")
  }
}

plugins {
  idea apply true
  kotlin("jvm") version "1.3.41"
  id("org.jetbrains.intellij") version "0.4.10"
  id("org.jetbrains.gradle.plugin.idea-ext") version "0.3" apply true
}

idea.project {
  (this as ExtensionAware)
  configure<ProjectSettings> {
    runConfigurations {
      create<org.jetbrains.gradle.ext.Application>("Run AceJump") {
        beforeRun.create<GradleTask>("runIde") {
          task = tasks.getByPath("runIde")
        }
      }
    }
  }
}

dependencies {
  // gradle-intellij-plugin doesn't attach sources properly for Kotlin :(
  compileOnly(kotlin("stdlib-jdk8"))
  compile("org.bytedeco:javacv-platform:1.5.1")
  compile("org.bytedeco:tesseract-platform:1.5.1")
  compile("org.bytedeco:leptonica-platform:1.5.1")
  implementation("com.1stleg:jnativehook:2.1.0")
  implementation("org.openjfx:javafx-swing:11")
  implementation("org.openjfx:javafx:11")
}

repositories.mavenCentral()

intellij {
  pluginName = "AceJump"
  updateSinceUntilBuild = false
  setPlugins("java")
}

group = "org.acejump"
version = "3.6.0"