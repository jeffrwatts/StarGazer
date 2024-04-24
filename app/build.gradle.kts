import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    //id("com.google.devtools.ksp") version "1.9.10-1.0.13"
    kotlin("kapt") // Needed for hilt currently.
    id("com.google.dagger.hilt.android")
    kotlin("plugin.allopen") version "1.9.10"
}

allOpen {
    annotation("com.jeffrwatts.stargazer.Mockable")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.jeffrwatts.stargazer"
    compileSdk = 34

    val apiUrl = localProperties.getProperty("api_base_url", "https://default.api.url/")

    defaultConfig {
        applicationId = "com.jeffrwatts.stargazer"
        minSdk = 33
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        // Enable custom BuildConfig fields
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", apiUrl)
        }

        debug {
            buildConfigField("String", "API_BASE_URL", apiUrl)
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")

    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.06.01"))

    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation ("androidx.work:work-runtime-ktx:2.9.0")


    // CameraX
    implementation ("androidx.camera:camera-camera2:1.3.2")
    implementation ("androidx.camera:camera-lifecycle:1.3.2")
    implementation ("androidx.camera:camera-view:1.4.0-alpha04")
    implementation ("androidx.camera:camera-extensions:$1.4.0-alpha04")

    implementation("io.coil-kt:coil:2.6.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Google Play Services for Location.
    implementation ("com.google.android.gms:play-services-location:21.2.0")

    // Dependency Injection
    implementation ("com.google.dagger:hilt-android:2.49")
    implementation ("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation ("androidx.hilt:hilt-work:1.2.0")
    implementation("androidx.test.ext:junit-ktx:1.1.5")
    kapt ("com.google.dagger:hilt-compiler:2.49")

    // Permissions
    implementation ("com.google.accompanist:accompanist-permissions:0.32.0")

    // Serialization
    implementation("com.google.code.gson:gson:2.9.0")

    // retrofit for networking
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.10.0")

    //Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    testImplementation ("androidx.room:room-testing:2.6.1")


    // Unit tests
    testImplementation("junit:junit:4.13.2")

    // Android Instrumentation tests
    androidTestImplementation ("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation ("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Mockito
    testImplementation ("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0") // Unit tests are broken if upgrade to 5.8.0.
    androidTestImplementation ("org.mockito:mockito-android:5.8.0")
}