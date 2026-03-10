package org.yarokovisty.stub.compiler

import org.gradle.api.Plugin
import org.gradle.api.Project

class StubCompilerGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            project.dependencies.add(
                "kotlinCompilerPluginClasspath",
                "io.github.yarokovisty:stub-compiler-plugin:0.4.0"
            )
        }
    }
}