plugins {
    alias(libs.plugins.stubAndroidLibrary)
    alias(libs.plugins.stubKotlinMultiplatform)
}

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "org.yarokovisty.stub.runtime"
}
