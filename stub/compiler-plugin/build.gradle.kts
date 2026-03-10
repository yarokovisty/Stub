plugins {
    kotlin("jvm") version "2.3.0"
    id("com.gradle.plugin-publish") version "2.0.0"
    alias(libs.plugins.mavenPublish)
}

dependencies {
    compileOnly(libs.kotlin.compiler)
    compileOnly(libs.kotlin.compiler.embeddable)
}

group = "io.github.yarokovisty"
version = "0.4.0"

gradlePlugin {
    website = "https://github.com/yarokovisty/Stub"
    vcsUrl = "https://github.com/yarokovisty/Stub"

    plugins {

        create("stubCompiler") {
            id = "io.github.yarokovisty.stub.compiler"
            displayName = "Stub Kotlin Compiler Plugin"
            description = "Kotlin compiler plugin for Stub mocking library"
            tags = listOf("testing", "integrationTesting")
            implementationClass = "org.yarokovisty.stub.compiler.StubCompilerGradlePlugin"
        }
    }
}
mavenPublishing {
    coordinates(
        groupId = "io.github.yarokovisty",
        artifactId = "stub-compiler-plugin",
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
