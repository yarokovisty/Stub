import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}

subprojects.onEach { project ->
    applyDetekt(project)
}

fun applyDetekt(project: Project) {
    project.plugins.apply(libs.plugins.detekt.get().pluginId)
    project.dependencies.add("detektPlugins", libs.detekt.formatting.get().toString())

    project.extensions.configure<DetektExtension> {
        val files = project.files(
            "src/main/kotlin",
            "src/commonMain/kotlin",
            "src/androidMain/kotlin",
            "src/jvmMain/kotlin"
        )
        source.setFrom(files)

        config.setFrom(rootProject.file("config/detekt/detekt.yml"))

        allRules = true
        buildUponDefaultConfig = true
    }
}
