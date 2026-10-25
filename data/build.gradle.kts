plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.hilt)
}

android {
  namespace = "com.example.data"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig { minSdk = 24 }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

dependencies {
  implementation(project(":core"))
  implementation(project(":domain"))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.cio)
  implementation(libs.ktor.client.content.negotiation)
  implementation(libs.ktor.serialization.kotlinx.json)
  implementation(libs.ktor.client.websockets)
  implementation(libs.hilt.android)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.hilt.compiler)
}
