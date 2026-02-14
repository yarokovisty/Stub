package org.yarokovisty.stub.buildlogic.convention

import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLibraryConventionPlugin : Plugin<Project> {

    private companion object {

        const val ANDROID_LIBRARY_PLUGIN_ID = "com.android.library"
        const val COMPILE_SDK_VERSION = "android-compileSdk"
        const val MIN_SDK_VERSION = "android-minSdk"
    }

    private val Project.android: LibraryExtension
        get() = extensions.getByType(LibraryExtension::class.java)

    override fun apply(project: Project) {
        applyPlugins(project)
        setConfigurations(project)
    }

    private fun applyPlugins(project: Project) {
        project.plugins.apply(ANDROID_LIBRARY_PLUGIN_ID)
    }

    private fun setConfigurations(project: Project) {
        project.android.apply {
            compileSdk = project.libs.getVersion(COMPILE_SDK_VERSION)

            defaultConfig {
                minSdk = project.libs.getVersion(MIN_SDK_VERSION)
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
        }
    }
}