dependencies {
    implementation(project(":core"))
    implementation(project(":content"))
    implementation(project(":engine"))
    implementation(project(":game"))
    implementation(project(":runtime"))
    implementation(project(":render"))

    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)
    implementation(libs.imgui.java.app)
}

tasks.register<JavaExec>("runGame") {
    group = "application"
    description = "Launches the Terralite game client."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.terralite.launcher.TerraliteGame")
    args(rootProject.layout.projectDirectory.file("packs").asFile.absolutePath)
    if (System.getProperty("os.name").startsWith("Mac")) {
        jvmArgs("-XstartOnFirstThread")
    }
}
