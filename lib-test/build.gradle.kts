plugins {
    id("aap.conventions")
}

dependencies {
    implementation(project(":flyt"))
    implementation(project(":klienter"))
    implementation(project(":repository"))

    implementation(libs.tilgangKontrakt)
    implementation(libs.behandlingsflytKontrakt)
    implementation(libs.arenaoppslagKontrakt)
    implementation(libs.httpklient)
    implementation(libs.dbconnect)
    implementation(libs.dbtest)
    implementation(libs.server)

    implementation(libs.jacksonDatabind)
    implementation(libs.jacksonDatatypeJsr310)
    implementation(libs.bundles.junit)

    implementation(libs.joseJwt)
}