import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.localentertainmentgames.homeinventory.shared"
        compileSdk = 37
        minSdk = 26
        androidResources { enable = true }
        withHostTest { }
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    // JVM target exists so Compose UI tests in commonTest run fast on the host.
    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }

    compilerOptions {
        optIn.addAll(
            "kotlin.uuid.ExperimentalUuidApi",
            "kotlin.time.ExperimentalTime",
            "androidx.compose.material3.ExperimentalMaterial3Api",
        )
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.io.core)
            implementation(libs.coil.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(libs.compose.ui.test)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.androidx.camera.mlkit.vision)
            implementation(libs.mlkit.barcode.scanning)
        }
        jvmTest.dependencies {
            implementation(libs.compose.desktop.macos.arm64)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.localentertainmentgames.homeinventory.resources"
    generateResClass = always
}

// Compose UI tests need a window: they run on the JVM and the iOS simulator. Android host tests run the logic tests.
tasks.matching { it.name == "testAndroidHostTest" }.configureEach {
    (this as Test).filter { excludeTestsMatching("*AppUiTest*") }
}
