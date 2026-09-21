plugins {
    id("aap.conventions")
}

dependencies {
    api(project(":flyt"))
    implementation(libs.dbconnect)
    implementation(libs.infrastructure)
    implementation(libs.server)
    implementation(libs.motor.api)
    implementation(libs.verdityper)
    api(libs.tilgang.plugin)
    api(libs.tilgang.kontrakt)
    api(libs.behandlingsflyt.kontrakt)
    compileOnly(kelvinLibs.ktor.http.jvm)

    testImplementation(kelvinLibs.bundles.junit)
}
