plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

    android {
    namespace = "com.vie.mit.database"
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
      implementation(libs.material)
      testImplementation(libs.junit)
      androidTestImplementation(libs.androidx.espresso.core)
      androidTestImplementation(libs.androidx.junit)

       //Timber
      implementation(libs.timber)
  }