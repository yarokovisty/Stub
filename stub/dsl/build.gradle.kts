plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        withSourcesJar(true)
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "StubDsl"
            isStatic = true
        }
    }

    js { nodejs() }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.stub.runtime)
        }
    }
}

android {
    namespace = "org.yarokovisty.stub.dsl"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    coordinates(
        groupId = "io.github.yarokovisty",
        artifactId = "stub-dsl",
        version = "0.4.0"
    )

    pom {
        name = "Stub"
        description = "The libreary for mocking in unit-tests in KMP."
        url = "https://github.com/yarokovisty/Stub"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/license-2.0.txt"
            }
        }

        issueManagement {
            system = "GitHub"
            url = "https://github.com/yarokovisty/Stub/issues"
        }

        scm {
            url = "https://github.com/yarokovisty/Stub"
            connection = "scm:git:git://github.com/yarokovisty/Stub.git"
            developerConnection = "scm:git:ssh://github.com/yarokovisty/Stub.git"
        }

        developers {
            developer {
                id = "YarokovistY"
                name = "Yaroslav Perov"
                email = "yarokovisty@gmail.com"
            }
        }
    }

    publishToMavenCentral()
    signAllPublications()
}