plugins {
    id("aap.conventions")
}

dependencies {
    api(project(":kontrakt"))
    api(libs.motor)
    api(libs.gateway)
    implementation(libs.behandlingsflyt.kontrakt)
    implementation(libs.arenaoppslag.kontrakt)
    implementation(libs.infrastructure)
    implementation(libs.httpklient)
    implementation(libs.motor.api)
    implementation(libs.verdityper)
    implementation(libs.kotlinx.coroutines.core)

    implementation(kotlin("reflect"))

    // Kafka
    implementation(libs.kafka.clients)
    implementation(libs.kafka.streams)
    implementation(libs.avro)
    implementation(libs.kafka.streams.avro.serde)
    implementation(libs.teamdokumenthandtering.avro.schemas)

    // https://github.com/navikt/teamdokumenthandtering-avro-schemas
    testImplementation(libs.kafka.streams.test.utils)
    testImplementation(libs.bundles.junit)
    testImplementation(project(":lib-test"))
    testImplementation(project(":repository"))
    testImplementation(project(":klienter"))
    testImplementation(project(":api"))
    testImplementation(libs.dbtest)
    testImplementation(libs.motor.test.utils)
    testImplementation(libs.mockk)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.junit)
}
