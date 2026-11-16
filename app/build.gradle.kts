plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.lostfoundcampus"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.lostfoundcampus"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        // BuildConfig.DEBUG masque l'interrupteur « mode auteur » dans les builds release.
        buildConfig = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    // Navigation Component : graphe des écrans et barre de navigation basse.
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    // ViewPager2 : galerie photos à défilement horizontal du détail d'annonce.
    implementation(libs.viewpager2)
    // osmdroid : cartes OpenStreetMap, sans clé d'API.
    implementation(libs.osmdroid)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
