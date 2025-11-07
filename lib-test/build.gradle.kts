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

    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.1")
    implementation(libs.bundles.junit)

    implementation("com.nimbusds:nimbus-jose-jwt:10.5")
}