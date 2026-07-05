plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "br.com.shopper.watchup"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "br.com.shopper.watchup"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
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
    // Core compartilhado + uma feature por tela do MVP.
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":feature:home"))
    implementation(project(":feature:library"))
    implementation(project(":feature:launches"))
    implementation(project(":feature:search"))
    implementation(project(":feature:detail"))
    implementation(project(":feature:registration"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)
}

// Gera uma cópia do APK debug com a versão no nome, em <raiz>/dist/.
// Cada versão vira um arquivo (e uma URL) diferente — facilita a distribuição
// e evita que o navegador do celular sirva uma versão antiga do cache.
tasks.register<Copy>("distApk") {
    dependsOn("assembleDebug")
    val version = android.defaultConfig.versionName
    from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
    into(rootProject.layout.projectDirectory.dir("dist"))
    rename { "watchup-$version-debug.apk" }
    doLast { println(">> APK versionado em: dist/watchup-$version-debug.apk") }
}
