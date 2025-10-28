plugins {
    id("postmottak.conventions")
}

dependencies {
    api(project(":kontrakt"))
    api(libs.motor)
    api(libs.gateway)
    implementation(libs.behandlingsflytKontrakt)
    implementation(libs.apiInternKontrakt)
    implementation(libs.dbconnect)
    implementation(libs.infrastructure)
    implementation(libs.httpklient)
    implementation(libs.server)
    implementation(libs.motorApi)
    implementation(libs.verdityper)
    api(libs.tilgangPlugin)
    api(libs.tilgangKontrakt)
    compileOnly(libs.ktorHttpJvm)
    implementation("org.flywaydb:flyway-database-postgresql:11.15.0")

    implementation(kotlin("reflect"))
    implementation("com.zaxxer:HikariCP:7.0.2")

    // Kafka
    implementation(libs.kafkaClients)
    implementation(libs.kafkaStreams)
    implementation("org.apache.avro:avro:1.12.1")
    implementation("io.confluent:kafka-streams-avro-serde:8.0.0")
    implementation("no.nav.teamdokumenthandtering:teamdokumenthandtering-avro-schemas:1.1.6")

    // https://github.com/navikt/teamdokumenthandtering-avro-schemas
    testImplementation(libs.kafkaStreamsTestUtils)
    testImplementation(libs.bundles.junit)
    testImplementation(project(":lib-test"))
    testImplementation(project(":repository"))
    testImplementation(project(":klienter"))
    testImplementation(project(":api"))
    testImplementation(libs.dbtest)
    testImplementation(libs.motorTestUtils)
    testImplementation(libs.mockk)
    testImplementation(libs.testcontainersPostgres)
    testImplementation(libs.testcontainersKafka)
    testImplementation(libs.testcontainersJunit)
}
