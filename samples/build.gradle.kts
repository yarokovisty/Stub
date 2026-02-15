plugins {
    alias(libs.plugins.stubCompiler)
    alias(libs.plugins.stubAndroidLibrary)
    alias(libs.plugins.stubKotlinMultiplatform)
}

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(project(":stub:dsl"))

            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "org.yarokovisty.stub.samples"
}
