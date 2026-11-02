import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Supabase credentials live in local.properties (never committed):
//   SUPABASE_URL=https://xxxx.supabase.co
//   SUPABASE_PUBLISHABLE_KEY=sb_publishable_...
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
// A value comes from local.properties first, then from an environment variable of the same name
// (that is how GitHub Actions passes its secrets).
// Backslashes and quotes are stripped: they never appear in a URL or key, but a value copied from
// local.properties can carry "https\://" and would break the generated BuildConfig.java.
fun prop(name: String): String =
    (localProps.getProperty(name) ?: System.getenv(name) ?: "").replace("\\", "").replace("\"", "").trim()

// Version: -PappVersion=1.2.3 (set by the Release workflow). A local build is labelled 1.0.0-dev.
// versionCode grows with the version: 1.2.3 -> 1002003.
val appVersion: String = (findProperty("appVersion") as String?)?.trim()?.removePrefix("v")?.takeIf { it.isNotEmpty() }
    ?: "1.0.0-dev"
val appVersionCode: Int = appVersion.substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }.let { v ->
    (v.getOrElse(0) { 0 } * 1_000_000 + v.getOrElse(1) { 0 } * 1_000 + v.getOrElse(2) { 0 }).coerceAtLeast(1)
}

// Release signing: KEYSTORE_FILE (path relative to the repo root), KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD.
// Without a keystore the release APK is signed with the debug key so it still installs.
val hasReleaseKeystore: Boolean = prop("KEYSTORE_FILE").let { it.isNotEmpty() && rootProject.file(it).exists() }

// APK files are named Coinop-<version>-<build type>.apk instead of app-release.apk.
base { archivesName.set("Coinop-$appVersion") }

android {
    namespace = "com.coinop.budget"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.coinop.budget"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersion
        buildConfigField("String", "SUPABASE_URL", "\"${prop("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${prop("SUPABASE_PUBLISHABLE_KEY")}\"")
    }

    signingConfigs {
        create("release") {
            if (hasReleaseKeystore) {
                storeFile = rootProject.file(prop("KEYSTORE_FILE"))
                storePassword = prop("KEYSTORE_PASSWORD")
                keyAlias = prop("KEY_ALIAS")
                keyPassword = prop("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (hasReleaseKeystore) "release" else "debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.ktor.client.okhttp)
}
