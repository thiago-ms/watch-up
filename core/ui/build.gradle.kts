plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "br.com.watchup.core.ui"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        minSdk = 26
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
    }

    sourceSets["main"].kotlin.srcDir("src/main/kotlin")
}

dependencies {
    // Enums/domínio usados pelos componentes de status compartilhados.
    api(project(":core:data"))

    // Exposto via `api`: quem depende de :core:ui herda o BOM e o Material 3,
    // padronizando as versões de Compose em todos os módulos de feature.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.ui)
    api(libs.androidx.ui.graphics)
    api(libs.androidx.ui.tooling.preview)
    api(libs.androidx.foundation)
    api(libs.androidx.material3)
    api(libs.androidx.material.icons.extended)

    // Carregamento de imagens de URL (pôsteres do TMDB) exposto a todas as features.
    api(libs.coil.compose)

    debugImplementation(libs.androidx.ui.tooling)
}
