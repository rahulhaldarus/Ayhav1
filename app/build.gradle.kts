import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  id("com.android.application")
  id("com.google.devtools.ksp")
  id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
  id("com.google.gms.google-services")
}

val isGradle9 = gradle.gradleVersion.startsWith("9")
if (isGradle9) {
  apply(plugin = "org.jetbrains.kotlin.plugin.compose")
} else {
  apply(plugin = "org.jetbrains.kotlin.android")
}

android {
  namespace = "com.example"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.aistudio.ayha.pjzwnx"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  if (!isGradle9) {
    composeOptions {
      kotlinCompilerExtensionVersion = "1.5.8"
    }
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

ksp {
  arg("room.generateKotlin", "true")
}

dependencies {
  implementation(platform("androidx.compose:compose-bom:2024.04.01"))
  implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
  implementation("androidx.activity:activity-compose:1.8.2")
  implementation("androidx.compose.material:material-icons-core")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.core:core-ktx:1.12.0")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
  implementation("androidx.navigation:navigation-compose:2.7.7")
  implementation("androidx.room:room-ktx:2.6.1")
  implementation("androidx.room:room-runtime:2.6.1")
  implementation("io.coil-kt:coil-compose:2.6.0")
  implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
  implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("com.squareup.retrofit2:retrofit:2.9.0")
  testImplementation("androidx.compose.ui:ui-test-junit4")
  testImplementation("androidx.test:core:1.5.0")
  testImplementation("androidx.test.ext:junit:1.1.5")
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
  testImplementation("org.robolectric:robolectric:4.11.1")
  testImplementation("io.github.takahirom.roborazzi:roborazzi:1.12.0")
  testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.12.0")
  testImplementation("io.github.takahirom.roborazzi:roborazzi-junit-rule:1.12.0")
  androidTestImplementation(platform("androidx.compose:compose-bom:2024.04.01"))
  androidTestImplementation("androidx.compose.ui:ui-test-junit4")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
  androidTestImplementation("androidx.test.ext:junit:1.1.5")
  androidTestImplementation("androidx.test:runner:1.5.2")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
  debugImplementation("androidx.compose.ui:ui-tooling")
  "ksp"("androidx.room:room-compiler:2.6.1")
  "ksp"("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
  }
}
