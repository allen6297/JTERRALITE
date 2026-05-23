dependencies {
    api(project(":core"))
    api(project(":content"))
    api(project(":engine"))

    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)
}
