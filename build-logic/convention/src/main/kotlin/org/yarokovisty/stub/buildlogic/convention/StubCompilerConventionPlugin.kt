package org.yarokovisty.stub.buildlogic.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

class StubCompilerConventionPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            project.dependencies.add(
                "kotlinCompilerPluginClasspath",
                project.dependencies.project(mapOf("path" to ":stub:compiler-plugin")),
            )
        }
    }
}
