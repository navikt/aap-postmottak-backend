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
    implementation(kelvinLibs.coroutines.core)

    implementation(kotlin("reflect"))

    // Kafka
    implementation(kelvinLibs.kafka.clients)
    implementation(kelvinLibs.kafka.streams)
    implementation(kelvinLibs.avro)
    implementation(libs.kafka.streams.avro.serde)
    implementation(libs.teamdokumenthandtering.avro.schemas)

    // https://github.com/navikt/teamdokumenthandtering-avro-schemas
    testImplementation(kelvinLibs.kafka.streams.test.utils)
    testImplementation(kelvinLibs.bundles.junit)
    testImplementation(project(":lib-test"))
    testImplementation(project(":repository"))
    testImplementation(project(":klienter"))
    testImplementation(project(":api"))
    testImplementation(libs.dbtest)
    testImplementation(libs.motor.test.utils)
    testImplementation(kelvinLibs.mockk)
    testImplementation(kelvinLibs.testcontainers.postgresql)
    testImplementation(kelvinLibs.testcontainers.kafka)
    testImplementation(kelvinLibs.testcontainers.junit.jupiter)
}
