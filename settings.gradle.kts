pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "JTERRALITE"
include("core")
include("launcher")
include("engine")
include("api")
include("content")
include("runtime")
include("server")
include("tools")
include("render")
include("game")
include("platform")
