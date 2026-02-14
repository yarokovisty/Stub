import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}

allprojects.onEach { project ->
    applyDetekt(project)
}

fun applyDetekt(project: Project) {
    project.plugins.apply(libs.plugins.detekt.get().pluginId)
    project.dependencies.add("detektPlugins", libs.detekt.formatting)

    project.extensions.configure<DetektExtension> {
        val files = project.files(
            "src/main/kotlin",
            "src/commonMain/kotlin",
            "src/androidMain/kotlin",
            "src/jvmMain/kotlin",
            "src/commonTest/kotlin",
        )
        source.setFrom(files)

        config.setFrom(rootProject.file("config/detekt/detekt.yml"))

        allRules = true
        buildUponDefaultConfig = true
    }
}
