plugins {
    id("aap.conventions")
}

dependencies {
    implementation(project(":flyt"))

    implementation(libs.logback.classic)
    implementation(libs.dbconnect)
    implementation(libs.verdityper)
    implementation(libs.dbmigrering)
    implementation(libs.infrastructure)
    implementation(libs.behandlingsflyt.kontrakt)

    testImplementation(project(":lib-test"))
    testImplementation(libs.dbtest)
    testImplementation(libs.bundles.junit)
}