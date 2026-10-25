pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "WishDrop"

include(
  ":app",
  ":core",
  ":domain",
  ":data",
  ":designsystem",
  ":navigation",
  ":feature_home",
  ":feature_gift_assistant",
  ":feature_wishlist",
  ":feature_product_details",
  ":feature_add_product",
  ":feature_insights",
  ":feature_profile",
  ":feature_shared_list",
)
