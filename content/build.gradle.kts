dependencies {
    api(project(":core"))

    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)
    implementation(libs.rhino)
}
