plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.example.navigation"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig { minSdk = 24 }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures { compose = true }
}

composeCompiler {
  stabilityConfigurationFiles.add(
    rootProject.layout.projectDirectory.file("compose_stability.conf")
  )
}

dependencies {
  implementation(project(":designsystem"))
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.animation)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.kotlinx.serialization.json)
}
