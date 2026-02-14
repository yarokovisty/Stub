package org.yarokovisty.stub.buildlogic.convention

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

private const val LIBS_VERSION = "libs"

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named(LIBS_VERSION)

internal fun VersionCatalog.getVersion(versionName: String): Int =
    findVersion(versionName).get().toString().toInt()