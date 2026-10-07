import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.aiguidecamera"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aiguidecamera"
        minSdk = 26
        targetSdk = 36
        // 諛고룷???뚮쭏???????щ┛?? ?깆? versionName??GitHub 由대━???쒓렇(v1.0 ??? 鍮꾧탳?쒕떎.
        versionCode = 9
        versionName = "1.0.8"

        // ?ㅼ젣 ?곗? 嫄곗쓽 紐⑤몢 64鍮꾪듃 ARM. ?ㅻⅨ CPU??ML Kit ?ㅼ씠?곕툕 ?쇱씠釉뚮윭由щ? 鍮쇱꽌 APK瑜?以꾩씤??
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    // 諛고룷???쒕챸 ?ㅻ뒗 ??μ냼???щ━吏 ?딅뒗??(keystore.properties, keystore/ ??.gitignore).
    // ?낅뜲?댄듃 ?ㅼ튂??媛숈? ?ㅻ줈 ?쒕챸??APK?쇰━留??섎?濡??ㅻ? ?껋뼱踰꾨━吏 ?딄쾶 ?곕줈 諛깆뾽?쒕떎.
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val releaseSigning = if (keystorePropertiesFile.exists()) {
        val properties = Properties().apply { keystorePropertiesFile.inputStream().use(::load) }
        signingConfigs.create("release") {
            storeFile = rootProject.file(properties.getProperty("storeFile"))
            storePassword = properties.getProperty("storePassword")
            keyAlias = properties.getProperty("keyAlias")
            keyPassword = properties.getProperty("keyPassword")
        }
    } else {
        null
    }

    buildTypes {
        release {
            signingConfig = releaseSigning
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.navigation.compose)

    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.coil.compose)

    implementation(libs.mlkit.face.detection)
    implementation(libs.mlkit.pose.detection)

    testImplementation(libs.junit)
}
