plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

// ktlint runs as the official CLI jar, not as a Gradle plugin (ADR 0001).
val ktlint: Configuration by configurations.creating

dependencies {
    ktlint(variantOf(libs.ktlint.cli) { classifier("all") })
}

val ktlintSources = listOf("**/src/**/*.kt", "*.kts", "app/*.kts", "!**/build/**")

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Checks Kotlin code style with ktlint."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(ktlintSources)
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Fixes Kotlin code style with ktlint."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(listOf("--format") + ktlintSources)
}
