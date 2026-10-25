plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.hilt)
}

android {
  namespace = "com.example.feature.addproduct"
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
  implementation(project(":designsystem"))
  implementation(project(":navigation"))
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.animation)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.hilt.android)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.hilt.compiler)
}
