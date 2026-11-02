plugins {
    id("com.android.application")
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("BILI_KEYSTORE"))
            storePassword = System.getenv("BILI_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("BILI_KEY_ALIAS")
            keyPassword = System.getenv("BILI_KEY_PASSWORD")
        }
    }
    namespace = "io.github.bilisplashskip"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.bilisplashskip"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.1"
        testInstrumentationRunner = "android.test.InstrumentationTestRunner"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
