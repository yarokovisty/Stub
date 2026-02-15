plugins {
    alias(libs.plugins.stubAndroidLibrary)
    alias(libs.plugins.stubKotlinMultiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":stub:runtime"))
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "org.yarokovisty.stub.dsl"
}
