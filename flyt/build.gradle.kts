plugins {
    id("postmottak.conventions")
}

val komponenterVersjon = "1.0.123"
val behandlingsflytVersjon = "0.0.132"
val ktorVersion = "3.0.3"
val tilgangVersjon = "0.0.94"
val junitVersjon = "5.11.4"
val kafkaVersion = "3.7.0"

dependencies {
    api(project(":kontrakt"))
    api("no.nav.aap.kelvin:motor:$komponenterVersjon")
    implementation("no.nav.aap.behandlingsflyt:kontrakt:$behandlingsflytVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor-api:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:verdityper:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:tidslinje:$komponenterVersjon")
    api("no.nav.aap.tilgang:plugin:$tilgangVersjon")
    api("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")
    compileOnly("io.ktor:ktor-http-jvm:$ktorVersion")
    implementation("org.flywaydb:flyway-database-postgresql:11.1.0")

    implementation(kotlin("reflect"))
    implementation("com.zaxxer:HikariCP:6.2.1")

    // kafka
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")
    implementation("org.apache.kafka:kafka_2.13:$kafkaVersion")
    implementation("org.apache.avro:avro:1.12.0")
    implementation("io.confluent:kafka-streams-avro-serde:7.7.1")
    implementation("no.nav.teamdokumenthandtering:teamdokumenthandtering-avro-schemas:08271806")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersjon")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation(project(":lib-test"))
    testImplementation(project(":repository"))
    testImplementation(project(":klienter"))
    testImplementation(project(":api"))
    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    testImplementation("no.nav.aap.kelvin:motor-test-utils:$komponenterVersjon")
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation("org.testcontainers:postgresql:1.20.4")
}
