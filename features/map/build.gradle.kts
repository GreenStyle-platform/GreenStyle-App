plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
}

    android {
    namespace = "com.vie.mit.map"
        compileSdk = 35

        defaultConfig {
    minSdk = 24

      testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        kotlinOptions {
            jvmTarget = "17"
        }
    }

  dependencies {
      implementation(libs.androidx.appcompat)
      implementation(libs.androidx.core.ktx)
      testImplementation(libs.junit)
      androidTestImplementation(libs.androidx.espresso.core)
      androidTestImplementation(libs.androidx.junit)

      // Module
      implementation(project(":core:common"))

      // Compose
      implementation(platform(libs.androidx.compose.bom))
      implementation(libs.androidx.compose.ui)
      implementation(libs.androidx.compose.ui.graphics)
      implementation(libs.androidx.compose.ui.tooling.preview)
      debugImplementation(libs.androidx.compose.ui.tooling)
      implementation(libs.androidx.compose.material3)
      implementation(libs.androidx.compose.runtime)

      //navigation compose
      implementation(libs.androidx.navigation.compose)

      //DI
      implementation(libs.hilt.android)
      ksp(libs.hilt.compiler)

       //Timber
      implementation(libs.timber)
  }
