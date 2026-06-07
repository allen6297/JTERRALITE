plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvmToolchain(21)
    jvm()
    androidTarget()

    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.slf4j.api)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
        }

        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(project(":server"))
            implementation(project(":runtime"))
            runtimeOnly(libs.logback.classic)
            implementation(libs.sqldelight.sqlite.driver)
        }

        jvmTest.dependencies {
            implementation(libs.junit.jupiter)
            runtimeOnly(libs.junit.platform.launcher)
            runtimeOnly(libs.logback.classic)
        }
    }
}

android {
    namespace = "com.terralite.launcher"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// Android preview tooling — wired onto the debug classpath as the docs require
dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.terralite.launcher.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "TERRALITE Launcher"
            packageVersion = "1.0.0"
        }
    }
}

tasks.register<JavaExec>("runServer") {
    group = "application"
    description = "Directly launches a headless dedicated server, bypassing the launcher UI."
    val jvmTarget = kotlin.targets.getByName<org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget>("jvm")
    val compilation = jvmTarget.compilations["main"]
    dependsOn(compilation.compileAllTaskName)
    classpath = compilation.output.allOutputs + compilation.runtimeDependencyFiles
    mainClass.set("com.terralite.launcher.DirectLaunchKt")
    args("server", rootProject.layout.projectDirectory.file("packs").asFile.absolutePath)
}

sqldelight {
    databases {
        create("LauncherDatabase") {
            packageName.set("com.terralite.launcher")
        }
    }
}
