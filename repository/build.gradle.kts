plugins {
    id("aap.conventions")
}

dependencies {
    implementation(project(":flyt"))

    implementation(libs.logbackClassic)
    implementation(libs.dbconnect)
    implementation(libs.verdityper)
    implementation(libs.dbmigrering)
    implementation(libs.infrastructure)
    implementation(libs.behandlingsflytKontrakt)
    implementation(libs.flywayDatabasePostgresql)
    runtimeOnly(libs.postgresql)

    testImplementation(project(":lib-test"))
    testImplementation(libs.dbtest)
    testImplementation(libs.bundles.junit)
}