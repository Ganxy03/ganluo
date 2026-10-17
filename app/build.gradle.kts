plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.ganluo.jiyi"
    compileSdk = 35

    defaultConfig {
        applicationId = "cn.ganluo.jiyi"
        minSdk = 26
        targetSdk = 35
        versionCode = 13
        versionName = "1.0.12"

        // NDK ABI 过滤,减小 APK 体积
        // 1.0.11:砍掉 armeabi-v7a(<1% 设备,省 23MB);只留 arm64-v8a
        // 之后可以加 splits 进一步按 ABI 拆
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    // --- Release 签名配置(读取 ~/.gradle/gradle.properties)---
    signingConfigs {
        create("release") {
            val storeFilePath = (project.findProperty("GANLUO_STORE_FILE") as String?)
                ?: throw GradleException("缺少 GANLUO_STORE_FILE,请配置 ~/.gradle/gradle.properties")
            storeFile = file(storeFilePath)
            storePassword = project.findProperty("GANLUO_STORE_PASSWORD") as String?
            keyAlias = project.findProperty("GANLUO_KEY_ALIAS") as String?
            keyPassword = project.findProperty("GANLUO_KEY_PASSWORD") as String?
        }
    }

    // 默认后端:线上 https://y.bobay.top
    // 切换到本机后端:在项目根目录的 local.properties 末尾加一行:
    //   API_BASE_URL=http://10.0.2.2:8000   # 模拟器看 PC
    //   API_BASE_URL=http://192.168.x.x:8000 # 真机走 LAN
    //   API_BASE_URL=http://y.bobay.top      # 默认(线上)
    val defaultApiBase = "https://y.bobay.top"
    val apiBaseUrl = (project.findProperty("API_BASE_URL") as String?) ?: defaultApiBase

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            buildConfigField("String", "BUILD_FLAVOR", "\"debug\"")
            // Debug 用 debug key(从 ~/.gradle/gradle.properties 读)
            val debugKey = (project.findProperty("AMAP_KEY_DEBUG") as String?)
                ?: "REPLACE_ME_DEBUG_KEY"
            buildConfigField("String", "AMAP_KEY", "\"$debugKey\"")
            manifestPlaceholders["AMAP_KEY"] = debugKey
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", "\"$defaultApiBase\"")
            buildConfigField("String", "BUILD_FLAVOR", "\"release\"")
            // Release 用 release key
            val releaseKey = (project.findProperty("AMAP_KEY_RELEASE") as String?)
                ?: "REPLACE_ME_RELEASE_KEY"
            buildConfigField("String", "AMAP_KEY", "\"$releaseKey\"")
            manifestPlaceholders["AMAP_KEY"] = releaseKey
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // --- Core / Compose ---
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // --- Lifecycle / ViewModel ---
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // --- Navigation ---
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // --- Hilt ---
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // --- Network ---
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")

    // --- DataStore ---
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // --- Image ---
    implementation("io.coil-kt:coil-compose:2.7.0")

    // --- Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // --- Permissions ---
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // --- Work (future) ---
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // --- Splash ---
    implementation("androidx.core:core-splashscreen:1.0.1")

    // --- 高德 SDK ---
    // 3DMap + Search + Location 三合一 fat jar(11.3.100)
    // + 2 个 .so 在 src/main/jniLibs/{arm64-v8a,armeabi-v7a}
    // 不用 maven:官方仓库连不上
    implementation(files("libs/amap-3dmap-11.3.100.jar"))

    // --- Tooling ---
    debugImplementation("androidx.compose.ui:ui-tooling")
}
