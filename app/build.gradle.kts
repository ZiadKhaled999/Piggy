import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.security.KeyStore

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.oryno.piggy_ledger"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.oryno.piggy_ledger"
    minSdk = 24
    targetSdk = 36
    versionCode = 815
    versionName = "4.3.7"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystoreFile = file("release.keystore")
      val rawPass = System.getenv("KEYSTORE_PASSWORD")
      val rawAlias = System.getenv("KEY_ALIAS")
      val rawKeyPass = System.getenv("KEY_PASSWORD")

      val pass = rawPass?.trim()?.removeSurrounding("\"")?.removeSurrounding("'")
      val alias = rawAlias?.trim()?.removeSurrounding("\"")?.removeSurrounding("'")
      val keyPass = rawKeyPass?.trim()?.removeSurrounding("\"")?.removeSurrounding("'")

      if (keystoreFile.exists() && keystoreFile.length() > 0L) {
        var loadedKs: KeyStore? = null
        var usedStorePass: String? = null

        val passCandidates = listOfNotNull(pass, keyPass, "android").filter { it.isNotEmpty() }.distinct()
        for (p in passCandidates) {
          for (type in listOf(KeyStore.getDefaultType(), "JKS", "PKCS12")) {
            try {
              val ks = KeyStore.getInstance(type)
              keystoreFile.inputStream().use { input -> ks.load(input, p.toCharArray()) }
              loadedKs = ks
              usedStorePass = p
              break
            } catch (_: Exception) {}
          }
          if (loadedKs != null) break
        }

        if (loadedKs != null && usedStorePass != null) {
          val aliases = loadedKs.aliases().toList()
          var targetAlias: String? = null
          if (!alias.isNullOrEmpty()) {
            targetAlias = aliases.firstOrNull { it == alias }
              ?: aliases.firstOrNull { it.equals(alias, ignoreCase = true) }
              ?: aliases.firstOrNull { it.trim().equals(alias.trim(), ignoreCase = true) }
          }
          if (targetAlias == null && aliases.isNotEmpty()) {
            targetAlias = aliases.first()
          }

          if (targetAlias != null) {
            val keyPassCandidates = listOfNotNull(keyPass, usedStorePass, pass).filter { it.isNotEmpty() }.distinct()
            var usedKeyPass: String? = null
            for (kp in keyPassCandidates) {
              try {
                if (loadedKs.isKeyEntry(targetAlias)) {
                  loadedKs.getKey(targetAlias, kp.toCharArray())
                  usedKeyPass = kp
                  break
                }
              } catch (_: Exception) {}
            }
            if (usedKeyPass == null && keyPassCandidates.isNotEmpty()) {
              usedKeyPass = keyPassCandidates.first()
            }

            if (usedKeyPass != null) {
              storeFile = keystoreFile
              storePassword = usedStorePass
              keyAlias = targetAlias
              keyPassword = usedKeyPass
              logger.lifecycle("Configured release signing with keystore '${keystoreFile.name}' and alias '$targetAlias'.")
            }
          }
        }
      }
    }

    val debugKs = file("${rootDir}/debug.keystore")
    if (debugKs.exists() && debugKs.length() > 0L) {
      create("debugConfig") {
        storeFile = debugKs
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

      val releaseConfig = signingConfigs.findByName("release")
      val debugConfig = signingConfigs.findByName("debugConfig")

      if (releaseConfig?.storeFile != null) {
        signingConfig = releaseConfig
      } else if (debugConfig?.storeFile != null) {
        signingConfig = debugConfig
      } else {
        signingConfig = null
      }
    }
    debug {
      val debugConfig = signingConfigs.findByName("debugConfig")
      if (debugConfig?.storeFile != null) {
        signingConfig = debugConfig
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  lint {
    abortOnError = false
    checkReleaseBuilds = false
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.clear()
}

googleServices {
  missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN
}


// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
    implementation("com.vanniktech:android-image-cropper:4.6.0")
  implementation(libs.revenuecat.purchases)
  implementation(libs.revenuecat.purchases.ui)
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.biometric)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.fragment.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.clerk.android.ui)
  implementation(libs.lottie.compose)
  implementation(libs.markdown.renderer.m3)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.ktor.client.android)
  implementation(libs.ktor.client.content.negotiation)
  implementation(libs.ktor.client.logging)
  implementation(libs.ktor.serialization.kotlinx.json)
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.utils)
  implementation(libs.vico.compose)
  implementation(libs.vico.compose.m3)
  implementation(libs.vico.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.posthog.android)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services.auth)
  implementation(libs.googleid)
    
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
