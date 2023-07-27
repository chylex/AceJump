@file:Suppress("ConvertLambdaToReference")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.8.0"
	id("org.jetbrains.intellij") version "1.15.0"
}

group = "org.acejump"
version = "chylex-11"

repositories {
	mavenCentral()
}

kotlin {
	jvmToolchain(17)
}

intellij {
	version.set("2023.1")
	updateSinceUntilBuild.set(false)
	plugins.add("IdeaVIM:chylex-15")
	
	pluginsRepositories {
		custom("https://intellij.chylex.com")
	}
}

tasks.patchPluginXml {
	sinceBuild.set("231")
}

tasks.buildSearchableOptions {
	enabled = false
}

tasks.withType<KotlinCompile> {
	kotlinOptions.freeCompilerArgs = listOf(
		"-Xjvm-default=all"
	)
}
