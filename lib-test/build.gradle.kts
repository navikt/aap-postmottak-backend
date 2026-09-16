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

    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.bundles.junit)

    implementation(libs.jose.jwt)
}