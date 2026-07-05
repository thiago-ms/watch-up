import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Chave do TMDB: procura em -PtmdbApiKey, env TMDB_API_KEY ou local.properties
// (tmdb.apiKey). Fica fora do versionamento — nunca é commitada.
val tmdbApiKey: String = run {
    providers.gradleProperty("tmdbApiKey").orNull
        ?: System.getenv("TMDB_API_KEY")
        ?: rootProject.file("local.properties").takeIf { it.exists() }?.let { f ->
            Properties().apply { f.inputStream().use { load(it) } }.getProperty("tmdb.apiKey")
        }
        ?: ""
}

android {
    namespace = "br.com.watchup.feature.search"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        minSdk = 26
        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets["main"].kotlin.srcDir("src/main/kotlin")
}

dependencies {
    // :core:ui reexporta o BOM do Compose, o Material 3 e o :core:data (domínio/repositório).
    implementation(project(":core:ui"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
}
