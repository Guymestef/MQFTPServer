/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.meta.spatial.plugin)
}

android {
  namespace = "com.mqftpserver.app"
  compileSdk = 34

  defaultConfig {
    applicationId = "com.mqftpserver.app"
    minSdk = 29
    //noinspection ExpiredTargetSdkVersion
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Update the ndkVersion to the right version for your app
    // ndkVersion = "27.0.12077973"
    
    // Fix for 16 KB page size compatibility
    ndk {
      debugSymbolLevel = "NONE"
    }
  }

  packaging { 
    resources.excludes.add("META-INF/LICENSE")
    
    // 16 KB page size compatibility
    jniLibs {
      useLegacyPackaging = false
    }
  }

  lint { abortOnError = false }

  buildTypes {
    debug {
      isMinifyEnabled = false
      // Force 16 KB alignment for debug builds
      packaging {
        jniLibs {
          useLegacyPackaging = false
        }
      }
    }
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Force 16 KB alignment for release builds
      packaging {
        jniLibs {
          useLegacyPackaging = false
        }
      }
    }
  }
  buildFeatures { buildConfig = true }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions { jvmTarget = "17" }
  
  // Additional NDK configuration for 16KB page size compatibility
  androidComponents {
    onVariants(selector().all()) { variant ->
      variant.packaging.jniLibs.useLegacyPackaging.set(false)
    }
  }
}

//noinspection UseTomlInstead
dependencies {
  implementation(libs.androidx.core.ktx)
  implementation("androidx.recyclerview:recyclerview:1.3.2")
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)

  // Meta Spatial SDK libs
  implementation(libs.meta.spatial.sdk.base)
  implementation(libs.meta.spatial.sdk.ovrmetrics)
  implementation(libs.meta.spatial.sdk.toolkit)
  implementation(libs.meta.spatial.sdk.vr)
  implementation(libs.meta.spatial.sdk.isdk)
  implementation(libs.meta.spatial.sdk.castinputforward)
  implementation(libs.meta.spatial.sdk.hotreload)
  implementation(libs.meta.spatial.sdk.datamodelinspector)
}
