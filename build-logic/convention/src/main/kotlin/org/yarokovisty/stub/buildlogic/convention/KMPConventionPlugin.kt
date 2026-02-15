package org.yarokovisty.stub.buildlogic.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KMPConventionPlugin : Plugin<Project> {

    private companion object {

        const val ANDROID_LIBRARY_PLUGIN_ID = "com.android.library"
        const val KOTLIN_MULTIPLATFORM_PLUGIN_ID = "org.jetbrains.kotlin.multiplatform"
    }

    override fun apply(project: Project) {
        applyPlugins(project)
        setConfigurations(project)
    }

    private fun applyPlugins(project: Project) = with(project.plugins) {
        apply(ANDROID_LIBRARY_PLUGIN_ID)
        apply(KOTLIN_MULTIPLATFORM_PLUGIN_ID)
    }

    private fun setConfigurations(project: Project) {
        project.kotlin().apply {
            androidTarget {
                @OptIn(ExperimentalKotlinGradlePluginApi::class)
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }

            iosX64()
            iosArm64()
            iosSimulatorArm64()

            js { nodejs() }

            jvm()
        }
    }

    private fun Project.kotlin(): KotlinMultiplatformExtension =
        extensions.getByType(KotlinMultiplatformExtension::class.java)
}