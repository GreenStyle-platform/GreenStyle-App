plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

    android {
    namespace = "com.vie.mit.data"
        compileSdk = 35

        defaultConfig {
    minSdk = 24

      testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
      consumerProguardFiles("consumer-rules.pro")
    }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
        kotlinOptions {
            jvmTarget = "11"
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

      //DI
      implementation(libs.hilt.android)
      ksp(libs.hilt.compiler)

      //Retrofit
      implementation(libs.retrofit)
      implementation(libs.retrofit.converter.kotlinx.serialization)
      implementation(libs.okhttp)
      //Kotlinx serialization
      implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
      //Data store
      implementation(libs.androidx.datastore.preferences)
  }