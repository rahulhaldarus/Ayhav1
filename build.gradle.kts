// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
  val isGradle9 = gradle.gradleVersion.startsWith("9")
  val agpVer = if (isGradle9) "9.1.1" else "8.1.4"
  val kotlinVer = if (isGradle9) "2.0.21" else "1.9.22"
  val kspVer = if (isGradle9) "2.0.21-1.0.28" else "1.9.22-1.0.17"

  repositories {
    google()
    mavenCentral()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:$agpVer")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVer")
    classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:$kspVer")
    classpath("com.google.android.libraries.mapsplatform.secrets-gradle-plugin:secrets-gradle-plugin:2.0.1")
    classpath("com.google.gms:google-services:4.4.0")
    if (isGradle9) {
      classpath("org.jetbrains.kotlin.plugin.compose:org.jetbrains.kotlin.plugin.compose.gradle.plugin:$kotlinVer")
    }
  }
}
