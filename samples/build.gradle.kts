plugins {
    alias(libs.plugins.stubAndroidLibrary)
    alias(libs.plugins.stubKotlinMultyplatform)
}

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(project(":stub:dsl"))
        }
    }
}

android {
    namespace = "org.yarokovisty.stub.samples"
}
