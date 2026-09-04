plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

    android {
    namespace = "com.vie.mit.network"
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

      //DI
      implementation(libs.hilt.android)
      ksp(libs.hilt.compiler)

      //Retrofit
      implementation(libs.retrofit)
      implementation(libs.retrofit.converter.kotlinx.serialization)
      implementation(libs.okhttp)
      //Kotlinx serialization
      implementation(libs.kotlinx.serialization.json)
  }