plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvm()
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
                implementation(project(":content"))
                implementation(project(":engine"))
                implementation(project(":game"))
                implementation(project(":render"))
                implementation(project(":runtime"))
                implementation(project(":server"))
                
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines)
                
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.slf4j.api)
                implementation(libs.logback.classic)
                implementation(project.dependencies.platform(libs.jackson.bom))
                implementation(libs.jackson.databind)
                
                // LWJGL for the game client
                implementation(project.dependencies.platform(libs.lwjgl.bom))
                implementation(libs.lwjgl)
                implementation(libs.lwjgl.glfw)
                implementation(libs.lwjgl.vulkan)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.sqldelight.android.driver)
            }
        }
    }
}

android {
    namespace = "com.terralite.launcher"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {1
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

afterEvaluate {
    tasks.findByName("jvmRun")?.let {
        (it as JavaExec).workingDir = rootProject.projectDir
    }
}

sqldelight {
    databases {
        register("LauncherDatabase") {
            packageName.set("com.terralite.launcher")
        }
    }
}
