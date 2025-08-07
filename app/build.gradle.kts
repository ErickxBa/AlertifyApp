import java.util.Properties


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.proyecto.alertify.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.proyecto.alertify.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY", "")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // AndroidX & Material Design
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Google Play Services for Maps & Location
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // Firebase - Import the BoM (Bill of Materials)
    implementation(platform(libs.firebase.bom.v3311))

    // Declare Firebase dependencies without versions
    implementation(libs.google.firebase.auth.ktx)
    implementation(libs.google.firebase.firestore.ktx)

    // Google Play Services - Import the BoM
    implementation(libs.play.services.location)
    // Declare Play Services dependencies without versions
    implementation(libs.play.services.auth)
    implementation(libs.places)
    implementation(libs.okhttp)
    implementation(libs.android.maps.utils)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.recyclerview)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    //Facebook Services
    implementation(libs.facebook.login)


    //Load Images
    implementation("com.github.bumptech.glide:glide:4.16.0")


    //Json
    implementation("com.google.code.gson:gson:2.10.1")
}