plugins {
    id("postmottak.conventions")
}

dependencies {
    implementation(project(":flyt"))

    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation(libs.dbconnect)
    implementation(libs.verdityper)
    implementation(libs.dbmigrering)
    implementation(libs.httpklient)
    implementation(libs.infrastructure)
    implementation(libs.behandlingsflytKontrakt)
    implementation("org.flywaydb:flyway-database-postgresql:11.13.2")
    runtimeOnly("org.postgresql:postgresql:42.7.8")

    testImplementation(project(":lib-test"))
    testImplementation(libs.dbtest)
    testImplementation(libs.bundles.junit)
}