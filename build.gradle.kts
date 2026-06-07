import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

subprojects {
    group = "com.terralite"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    // java-library, toolchain, and JUnit deps apply to every module EXCEPT the KMP launcher,
    // which configures these itself via its kotlin {} source sets.
    if (name != "launcher") {
        apply(plugin = "java-library")

        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(versionCatalog.findVersion("java").get().requiredVersion))
            }
        }

        dependencies {
            add("implementation", versionCatalog.findLibrary("slf4j-api").get())

            add("testImplementation", platform(versionCatalog.findLibrary("junit-bom").get()))
            add("testImplementation", versionCatalog.findLibrary("junit-jupiter").get())
            add("testRuntimeOnly", versionCatalog.findLibrary("junit-platform-launcher").get())
            add("testRuntimeOnly", versionCatalog.findLibrary("logback-classic").get())
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}

tasks.register("check") {
    group = "verification"
    description = "Runs repository-level verification checks."
    dependsOn(subprojects.map { it.tasks.named("check") })
    dependsOn(":tools:checkTypeScriptApi")
}
