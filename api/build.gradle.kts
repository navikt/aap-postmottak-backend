plugins {
    id("postmottak.conventions")
}

dependencies {
    api(project(":flyt"))
    implementation(libs.dbconnect)
    implementation(libs.infrastructure)
    implementation(libs.server)
    implementation(libs.motorApi)
    implementation(libs.verdityper)
    api(libs.tilgangPlugin)
    api(libs.tilgangKontrakt)
    api(libs.behandlingsflytKontrakt)
    compileOnly(libs.ktorHttpJvm)

    testImplementation(libs.bundles.junit)
}
