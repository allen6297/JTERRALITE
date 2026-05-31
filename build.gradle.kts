import org.gradle.api.artifacts.VersionCatalogsExtension

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

subprojects {
    apply(plugin = "java-library")

    group = "com.terralite"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

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

tasks.register("check") {
    group = "verification"
    description = "Runs repository-level verification checks."
    dependsOn(":tools:checkTypeScriptApi")
}
