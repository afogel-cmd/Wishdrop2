plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.example.designsystem"
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
  implementation(project(":core"))
  implementation(project(":domain"))
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.compose.animation)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.coil.compose)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
