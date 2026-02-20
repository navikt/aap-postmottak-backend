plugins {
    id("aap.conventions")
}

dependencies {
    api(project(":kontrakt"))
    api(libs.motor)
    api(libs.gateway)
    implementation(libs.behandlingsflytKontrakt)
    implementation(libs.apiInternKontrakt)
    implementation(libs.infrastructure)
    implementation(libs.httpklient)
    implementation(libs.motorApi)
    implementation(libs.verdityper)

    implementation(kotlin("reflect"))

    // Kafka
    implementation(libs.kafkaClients)
    implementation(libs.kafkaStreams)
    implementation(libs.avro)
    implementation(libs.kafkaStreamsAvroSerde)
    implementation(libs.teamdokumenthandteringAvroSchemas)

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
