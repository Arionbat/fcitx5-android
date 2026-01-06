plugins {
    id("org.fcitx.fcitx5.android.app-convention")
    id("org.fcitx.fcitx5.android.plugin-app-convention")
    id("org.fcitx.fcitx5.android.build-metadata")
    id("org.fcitx.fcitx5.android.data-descriptor")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.fcitx.fcitx5.android.plugin.sync"

    buildFeatures {
        buildConfig = true
    }
    
    defaultConfig {
        applicationId = "org.fcitx.fcitx5.android.plugin.sync"
    }

    buildTypes {
        release {
            resValue("string", "app_name", "@string/app_name_release")
        }
        debug {
            resValue("string", "app_name", "@string/app_name_debug")
        }
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(project(":lib:plugin-base"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
}
