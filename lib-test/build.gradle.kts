plugins {
    id("postmottak.conventions")
}

dependencies {
    implementation(project(":flyt"))
    implementation(project(":klienter"))
    implementation(project(":repository"))

    implementation(libs.tilgangKontrakt)
    implementation(libs.behandlingsflytKontrakt)
    implementation(libs.httpklient)
    implementation(libs.dbconnect)
    implementation(libs.dbtest)
    implementation(libs.ktorServerContentNegotation)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerCors)
    implementation(libs.ktorServerStatusPages)
    implementation(libs.ktorSerializationJackson)

    implementation(libs.jacksonDatabind)
    implementation(libs.jacksonDatatypeJsr310)
    implementation(libs.bundles.junit)

    implementation(libs.joseJwt)
}