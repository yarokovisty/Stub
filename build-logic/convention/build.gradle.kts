plugins {
    `kotlin-dsl`
}

group = "org.yarokovisty.stub.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        create("kmpPlugin") {
            id = "org.yarokovisty.stub.kmp"
            implementationClass = "org.yarokovisty.stub.buildlogic.convention.KMPConventionPlugin"
        }

        create("androidLibraryPlugin") {
            id = "org.yarokovisty.stub.androidLibrary"
            implementationClass = "org.yarokovisty.stub.buildlogic.convention.AndroidLibraryConventionPlugin"
        }

        create("stubCompilerPlugin") {
            id = "org.yarokovisty.stub.compiler"
            implementationClass = "org.yarokovisty.stub.buildlogic.convention.StubCompilerConventionPlugin"
        }
    }
}
