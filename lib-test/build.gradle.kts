plugins {
    id("aap.conventions")
}

dependencies {
    implementation(project(":flyt"))
    implementation(project(":klienter"))
    implementation(project(":repository"))

    implementation(libs.tilgang.kontrakt)
    implementation(libs.behandlingsflyt.kontrakt)
    implementation(libs.arenaoppslag.kontrakt)
    implementation(libs.httpklient)
    implementation(libs.dbconnect)
    implementation(libs.dbtest)
    implementation(libs.server)

    implementation(kelvinLibs.jackson.databind)
    implementation(kelvinLibs.jackson.datatype.jsr310)
    implementation(kelvinLibs.bundles.junit)

    implementation(kelvinLibs.nimbus.jose.jwt)
}