// Sample: how a launcher embeds DiPlay's live dashboard map (inactive in this fork).
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.diplay.maphost"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.diplay.maphost"
        minSdk = 30 // SurfaceView.getHostToken and setChildSurfacePackage
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
