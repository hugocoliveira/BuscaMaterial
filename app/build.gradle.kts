import java.util.Properties

// Lê local.properties para obter credenciais de assinatura (não commitado no git)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace   = "br.com.lit.busca.material"
    compileSdk  = 36

    defaultConfig {
        applicationId   = "br.com.lit.busca.material"
        minSdk          = 24
        targetSdk       = 36
        versionCode     = 10
        versionName     = "1.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Credenciais SAP — lidas do local.properties (gitignored), nunca expostas no código-fonte
        buildConfigField("String", "SAP_USERNAME", "\"${localProps.getProperty("sap.username", "")}\"")
        buildConfigField("String", "SAP_PASSWORD", "\"${localProps.getProperty("sap.password", "")}\"")

        // Vetor de suporte para API < 21 (redundante aqui mas boa prática)
        vectorDrawables { useSupportLibrary = true }
        // Apenas arm64 e arm32 — exclui x86/x86_64 (não usados nos coletores Zebra)
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    // Assinatura release — valores vêm do local.properties, nunca hardcoded no git
    signingConfigs {
        create("release") {
            storeFile     = file(localProps.getProperty("keystore.path", ""))
            storePassword = localProps.getProperty("keystore.password", "")
            keyAlias      = localProps.getProperty("key.alias", "")
            keyPassword   = localProps.getProperty("key.password", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose      = true
        buildConfig  = true
    }

    composeOptions {
        // Compatível com Kotlin 1.9.x
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.activity.compose)

    // Compose BOM — define versões de todos os artefatos Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // Rede
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // ML Kit — leitura de QR Code e código de barras
    implementation(libs.mlkit.barcode)

    // Coroutines
    implementation(libs.coroutines.android)

    // Accompanist — gerenciamento de permissões em Compose
    implementation(libs.accompanist.permissions)
}
