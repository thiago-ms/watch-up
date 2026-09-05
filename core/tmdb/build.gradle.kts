import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

// Chave do TMDB: procura em -PtmdbApiKey, env TMDB_API_KEY ou local.properties
// (tmdb.apiKey). Fica fora do versionamento — nunca é commitada. Veio do
// :feature:search quando o cliente subiu para cá, para que :feature:registration
// também pudesse consultar a API sem depender de outra feature.
val tmdbApiKey: String = run {
    providers.gradleProperty("tmdbApiKey").orNull
        ?: System.getenv("TMDB_API_KEY")
        ?: rootProject.file("local.properties").takeIf { it.exists() }?.let { f ->
            Properties().apply { f.inputStream().use { load(it) } }.getProperty("tmdb.apiKey")
        }
        ?: ""
}

android {
    namespace = "br.com.watchup.core.tmdb"
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

    // Sem Compose: este módulo é cliente de rede, não tem UI.
    buildFeatures {
        buildConfig = true
    }

    sourceSets["main"].kotlin.srcDir("src/main/kotlin")
    sourceSets["test"].kotlin.srcDir("src/test/kotlin")
}

dependencies {
    // O cliente normaliza os resultados para o vocabulário do app (TipoMidia), e a
    // chave em uso vem de TmdbPrefs — os dois moram no :core:data.
    implementation(project(":core:data"))

    implementation(libs.kotlinx.coroutines.android)

    // Só o TmdbMapa é testável aqui: é lógica pura. O parser depende de org.json,
    // que o classpath de unit test não fornece.
    testImplementation(libs.junit)
}
