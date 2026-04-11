//import java.util.Properties
//plugins {
//    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlin.android)
//    alias(libs.plugins.kotlin.compose)
//    // 🔥 修复2：必加这个插件！！！
//    id("kotlin-parcelize")
//}
//
//val localProperties = Properties()
//val localFile = rootProject.file("local.properties")
//if (localFile.exists()) {
//    localFile.inputStream().use { stream ->
//        localProperties.load(stream)
//    }
//}
//
//val amapWebKey = localProperties.getProperty("AMAP_WEB_KEY", "")
//val amapAndroidKey = localProperties.getProperty("AMAP_ANDROID_KEY", "")
//val apiBaseUrl = localProperties.getProperty("API_BASE_URL", "http://10.0.2.2:3000/")
//
//android {
//    namespace = "com.example.netgeocourier"
//    compileSdk = 35
//
//    buildFeatures {
//        buildConfig = true
//    }
//
//    defaultConfig {
//        applicationId = "com.example.netgeocourier"
//        minSdk = 26
//        targetSdk = 34
//        versionCode = 1
//        versionName = "1.0"
//
//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//        buildConfigField("String", "AMAP_WEB_KEY", "\"$amapWebKey\"")
//        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
//        manifestPlaceholders["AMAP_KEY"] = amapAndroidKey
//    }
//
//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//            signingConfig = signingConfigs.getByName("debug")
//        }
//    }
//
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_21
//        targetCompatibility = JavaVersion.VERSION_21
//    }
//
//    kotlinOptions {
//        jvmTarget = "21"
//    }
//
//    buildFeatures {
//        compose = true
//        buildConfig = true
//    }
//}
//
//dependencies {
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.lifecycle.runtime.ktx)
//
//    implementation(libs.androidx.activity.compose)
//    implementation(platform(libs.androidx.compose.bom))
//    implementation(libs.androidx.compose.runtime.saveable)
//
//    implementation(libs.androidx.ui)
//    implementation(libs.androidx.ui.graphics)
//    implementation(libs.androidx.ui.tooling.preview)
//    implementation(libs.androidx.material3)
//    // rememberSaveable 必需依赖
//
//    //implementation("com.google.android.gms:play-services-location:21.0.1")
//    implementation(libs.amap.location)
//    implementation("androidx.fragment:fragment-ktx:1.6.2")
//    implementation("com.squareup.retrofit2:retrofit:2.11.0")
//    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
//    implementation("com.squareup.okhttp3:okhttp:4.12.0")
//
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.ui.test.junit4)
//    debugImplementation(libs.androidx.ui.tooling)
//    debugImplementation(libs.androidx.ui.test.manifest)
//
//
//
//}
import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize") // 保留，必须有
}

val localProperties = Properties()
val localFile = rootProject.file("local.properties")
if (localFile.exists()) {
    localFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}

val amapWebKey = localProperties.getProperty("AMAP_WEB_KEY", "")
val amapAndroidKey = localProperties.getProperty("AMAP_ANDROID_KEY", "")
val apiBaseUrl = localProperties.getProperty("API_BASE_URL", "http://10.0.2.2:3000/")

android {
    namespace = "com.example.netgeocourier"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.netgeocourier"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "AMAP_WEB_KEY", "\"$amapWebKey\"")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        manifestPlaceholders["AMAP_KEY"] = amapAndroidKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    // ✅ 正确依赖，永不爆红！
    implementation("androidx.compose.runtime:runtime-saveable")

    implementation(libs.amap.location)
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}