plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.mockativePlugin) apply false
    alias(libs.plugins.sqlDelightPlugin) apply false
    alias(libs.plugins.kotlin.plugin.serialization) apply false
    alias(libs.plugins.mokkery) apply false
}