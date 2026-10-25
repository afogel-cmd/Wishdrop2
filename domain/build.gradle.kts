plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.example.domain"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig { minSdk = 24 }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

dependencies {
  implementation(project(":core"))
  implementation(libs.hilt.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.serialization.json)
}
