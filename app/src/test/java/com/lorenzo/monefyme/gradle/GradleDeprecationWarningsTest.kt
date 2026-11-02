package com.lorenzo.monefyme.gradle

import java.io.File
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the Gradle 9 upgrade preparation.
 *
 * The Kotlin Gradle plugin used to call Gradle APIs that are deprecated and are scheduled for
 * removal (StartParameter.isConfigurationCacheRequested and Configuration.fileCollection(Spec)).
 * Running a configuration-only invocation of the real build (the unit test task graph with
 * `--dry-run`, so nothing is executed) with `--warning-mode all` must not report any of those
 * deprecations any more; if a dependency bump brings them back this test fails.
 */
class GradleDeprecationWarningsTest {

    private val deprecatedUsages = listOf(
        "StartParameter.isConfigurationCacheRequested",
        "Configuration.fileCollection(Spec)",
    )

    @Test
    fun `configuration phase does not report Gradle 9 removal deprecations`() {
        val root = findProjectRoot()
        val output = runGradleConfigurationOnlyBuild(root)

        for (marker in deprecatedUsages) {
            assertFalse(
                "Gradle reported a deprecation for '$marker' while preparing for Gradle 9. " +
                    "Build output:\n$output",
                output.contains(marker),
            )
        }
    }

    @Test
    fun `compose compiler is applied through the kotlin compose plugin`() {
        // Kotlin 2.x moved the Compose compiler out of kotlinCompilerExtensionVersion; the app
        // must apply org.jetbrains.kotlin.plugin.compose instead of configuring composeOptions.
        val appBuild = File(findProjectRoot(), "app/build.gradle.kts").readText()
        assertTrue(
            "app/build.gradle.kts must apply org.jetbrains.kotlin.plugin.compose",
            appBuild.contains("org.jetbrains.kotlin.plugin.compose"),
        )
        assertFalse(
            "composeOptions { kotlinCompilerExtensionVersion = ... } is invalid with the Kotlin " +
                "Compose plugin",
            appBuild.contains("kotlinCompilerExtensionVersion"),
        )
    }

    private fun findProjectRoot(): File {
        val start = File(System.getProperty("user.dir") ?: ".").absoluteFile
        var dir: File? = start
        while (dir != null) {
            val current: File = dir
            if (File(current, "settings.gradle.kts").exists() &&
                (File(current, "gradlew").exists() || File(current, "gradlew.bat").exists())
            ) {
                return current
            }
            dir = current.parentFile
        }
        error("Could not locate the Gradle project root starting from ${System.getProperty("user.dir")}")
    }

    private fun runGradleConfigurationOnlyBuild(root: File): String {
        val isWindows = System.getProperty("os.name").orEmpty().lowercase().contains("windows")
        val command = if (isWindows) {
            listOf(
                "cmd", "/c", "gradlew.bat",
                ":app:testDebugUnitTest", "--dry-run",
                "--warning-mode", "all", "--offline", "--no-daemon", "--no-configuration-cache",
                "--console=plain",
            )
        } else {
            listOf(
                "./gradlew",
                ":app:testDebugUnitTest", "--dry-run",
                "--warning-mode", "all", "--offline", "--no-daemon", "--no-configuration-cache",
                "--console=plain",
            )
        }

        val process = ProcessBuilder(command)
            .directory(root)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val finished = process.waitFor(5, TimeUnit.MINUTES)
        if (!finished) {
            process.destroyForcibly()
            error("Gradle configuration-only build timed out. Output:\n$output")
        }

        check(process.exitValue() == 0) {
            "Gradle configuration-only build failed with exit code ${process.exitValue()}.\n$output"
        }
        return output
    }
}
