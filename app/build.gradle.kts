plugins {
    alias(libs.plugins.android.application)
}

    android {
    namespace = "com.vie.mit.green"
        compileSdk {
            version = release(36) {
                minorApiLevel = 1
            }
        }

        defaultConfig {
      applicationId = "com.vie.mit.green"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

      testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

  dependencies {
      implementation(libs.androidx.core.ktx)
      testImplementation(libs.junit)
      androidTestImplementation(libs.androidx.espresso.core)
      androidTestImplementation(libs.androidx.junit)
  }