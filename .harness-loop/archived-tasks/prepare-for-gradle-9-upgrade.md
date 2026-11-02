---
status: done
attempts: 2
title: "title"
source: backlog.md
depends_on: nothing
---

## Task
We are preparing to upgrade to Gradle 9; fix the following problems:
- The StartParameter.isConfigurationCacheRequested property has been deprecated. This is scheduled to be removed in Gradle 10.0. Please use 'configurationCache.requested' property on 'BuildFeatures' service instead. Consult the upgrading guide for further information: https://docs.gradle.org/8.14.6/userguide/upgrading_version_8.html#deprecated_startparameter_is_configuration_cache_requested
- The Configuration.fileCollection(Spec) method has been deprecated. This is scheduled to be removed in Gradle 9.0. Use Configuration.getIncoming().artifactView(Action) with a componentFilter instead. Consult the upgrading guide for further information: https://docs.gradle.org/8.14.6/userguide/upgrading_version_8.html#deprecate_filtered_configuration_file_and_filecollection_methods

## Run log

-   - runs the real build configuration phase (`gradlew :app:testDebugUnitTest --dry-run --warning-mode all --offline --no-daemon --no-configuration-cache`, no task execution, ~7 s) and fails if `StartParameter.isConfigurationCacheRequested` or `Configuration.fileCollection(Spec)` is reported again;
-   - asserts the Compose compiler is wired via the Kotlin Compose plugin and that `kotlinCompilerExtensionVersion` is gone.
-   - Placed in package `...monefyme.gradle` because `.gitignore`'s `**/build/` silently ignored a `...monefyme/build/` test folder.
-   **Verification run** (with `ANDROID_HOME` set as in `set-env-variables.ps1`)
-   - `./gradlew testDebugUnitTest --rerun-tasks --warning-mode all` → BUILD SUCCESSFUL, 86 tests, 0 failures / 0 skipped, and no "Deprecated Gradle features were used" line; after deleting the stale report, no `build/reports/problems/problems-report.html` is regenerated at all (0 problems).
-   - Negative check that the new test is not vacuous: with the previous `1.9.24` plugins restored it fails (`2 tests completed, 2 failed`), and `--dry-run` reproduced both deprecation messages, confirming the test's invocation catches both APIs.
-   - `./gradlew assembleDebug assembleDebugAndroidTest` → BUILD SUCCESSFUL (main, unit-test and androidTest sources compile under Kotlin 2.0.21).
-   **Caveats**
-   - kapt logs `w: Kapt currently doesn't support language version 2.0+. Falling back to 1.9.` — expected for kapt; migrating Room off kapt (KSP) is a separate task.
-   - `connectedDebugAndroidTest` was not run (no emulator available here).
-   - Remaining compile warnings in app sources (deprecated Compose `Divider`/`ArrowBack` icons, coroutines opt-in) are pre-existing and out of scope.
-   STATUS: DONE upgraded Kotlin Gradle plugins to 2.0.21 + Kotlin Compose plugin (build.gradle.kts, app/build.gradle.kts, composeOptions removed) which removes both Gradle deprecation warnings; added app/src/test/java/com/lorenzo/monefyme/gradle/GradleDeprecationWarningsTest.kt guard; verified with ./gradlew testDebugUnitTest (86 tests, 0 failures), --rerun-tasks run producing zero problems report, assembleDebug/assembleDebugAndroidTest, and a negative run proving the new test fails on the old plugin versions
-   Done. Root cause and fix:
-   **Source of the deprecations** — the problems report attributed all three warnings to `pluginId: org.jetbrains.kotlin.android` (Kotlin Gradle plugin 1.9.24), not to our own build scripts: `StartParameter.isConfigurationCacheRequested` and `Configuration.fileCollection(Spec)` were called inside KGP.
-   **Changes**
-   - `build.gradle.kts` — Kotlin Gradle plugins `org.jetbrains.kotlin.android` / `org.jetbrains.kotlin.kapt`: `1.9.24` → `2.0.21`; added `org.jetbrains.kotlin.plugin.compose` `2.0.21` (apply false). 2.0.21 was chosen deliberately: Kotlin 2.1.x/2.2.x emit metadata 2.1.0, which Room 2.6.1's kapt processor rejects (`maximum supported version is 2.0.0` → `kaptDebugKotlin` fails); bumping Room would be a wider change than this task.
-   - `app/build.gradle.kts` — apply `org.jetbrains.kotlin.plugin.compose`; removed the `composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }` block (invalid/obsolete with the Kotlin 2.x Compose plugin; `buildFeatures.compose = true` kept).
-   - `app/src/test/java/com/lorenzo/monefyme/gradle/GradleDeprecationWarningsTest.kt` (new) — regression guard:
-   - runs the real build configuration phase (`gradlew :app:testDebugUnitTest --dry-run --warning-mode all --offline --no-daemon --no-configuration-cache`, no task execution, ~7 s) and fails if `StartParameter.isConfigurationCacheRequested` or `Configuration.fileCollection(Spec)` is reported again;
-   - asserts the Compose compiler is wired via the Kotlin Compose plugin and that `kotlinCompilerExtensionVersion` is gone.
-   - Placed in package `...monefyme.gradle` because `.gitignore`'s `**/build/` silently ignored a `...monefyme/build/` test folder.
-   **Verification run** (with `ANDROID_HOME` set as in `set-env-variables.ps1`)
-   - `./gradlew testDebugUnitTest --rerun-tasks --warning-mode all` → BUILD SUCCESSFUL, 86 tests, 0 failures / 0 skipped, and no "Deprecated Gradle features were used" line; after deleting the stale report, no `build/reports/problems/problems-report.html` is regenerated at all (0 problems).
-   - Negative check that the new test is not vacuous: with the previous `1.9.24` plugins restored it fails (`2 tests completed, 2 failed`), and `--dry-run` reproduced both deprecation messages, confirming the test's invocation catches both APIs.
-   - `./gradlew assembleDebug assembleDebugAndroidTest` → BUILD SUCCESSFUL (main, unit-test and androidTest sources compile under Kotlin 2.0.21).
-   **Caveats**
-   - kapt logs `w: Kapt currently doesn't support language version 2.0+. Falling back to 1.9.` — expected for kapt; migrating Room off kapt (KSP) is a separate task.
-   - `connectedDebugAndroidTest` was not run (no emulator available here).
-   - Remaining compile warnings in app sources (deprecated Compose `Divider`/`ArrowBack` icons, coroutines opt-in) are pre-existing and out of scope.
-   STATUS: DONE upgraded Kotlin Gradle plugins to 2.0.21 + Kotlin Compose plugin (build.gradle.kts, app/build.gradle.kts, composeOptions removed) which removes both Gradle deprecation warnings; added app/src/test/java/com/lorenzo/monefyme/gradle/GradleDeprecationWarningsTest.kt guard; verified with ./gradlew testDebugUnitTest (86 tests, 0 failures), --rerun-tasks run producing zero problems report, assembleDebug/assembleDebugAndroidTest, and a negative run proving the new test fails on the old plugin versions
