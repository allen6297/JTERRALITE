dependencies {
    api(project(":core"))
    api(project(":content"))

    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)
}
