import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("com.android.application")
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

if (file("google-services.json").isFile) {
    apply(plugin = "com.google.gms.google-services")
}

kotlin {
    val composeAppXcFramework = XCFramework()

    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:domain"))
            implementation(project(":shared:data"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation("io.github.alexzhirkevich:compottie:2.0.0")
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.10.1")
        }
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            // Static: defers FirebaseCore/Analytics/Firestore symbol resolution to Xcode's
            // app-level link (where SPM already provides them), avoiding a standalone
            // Kotlin/Native link-time dependency on those frameworks.
            isStatic = true
            freeCompilerArgs += "-Xbinary=bundleId=com.impostor.composeapp"
            if (target.name != "iosX64") {
                composeAppXcFramework.add(this)
            }
        }
    }
}

android {
    namespace = "com.rafal.impostorparty"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rafal.impostorparty"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "1.0.4"
    }
    buildFeatures { compose = true }

    lint {
        // These detectors crash with the Kotlin 2.2 UAST used by the current AGP/toolchain.
        disable += "NullSafeMutableLiveData"
        disable += "FrequentlyChangingValue"
        disable += "RememberInComposition"
    }

    sourceSets["main"].assets.srcDir("../shared/data/src/commonMain/resources")
}

compose.resources { publicResClass = true }
