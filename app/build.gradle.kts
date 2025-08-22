plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.aio.jhl_all_in_one"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aio.jhl_all_in_one"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE.txt"
        }
    }
}

dependencies {

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.exifinterface)

    debugImplementation(libs.androidx.ui.tooling)

    // Compose testing
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Navigation
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.navigation.compose)

    configurations.configureEach {
        resolutionStrategy {
            force("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
            force("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
        }
    }

    implementation(libs.accompanist.permissions) // 최신 버전 확인


    // Google Sign-In
    implementation(libs.play.services.auth)

    // HTTP 클라이언트 (Gson 기반)
    implementation(libs.google.http.client.gson)

    // Google API 클라이언트
    implementation(libs.google.api.client.android) {
        exclude(group = "com.google.guava")
    }
    implementation(libs.google.api.services.sheets) {
        exclude(group = "com.google.guava")
    }

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)

    // com.mr0xf00.easycrop.ImageCropper r
    implementation(libs.easycrop)
    implementation(libs.android.image.cropper)

    // To recognize Korean script
    implementation(libs.text.recognition.korean)

    // Import the Firebase BoM
    // Firebase BOM (버전만 여기서 관리)
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))

    // Firestore + KTX
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
}