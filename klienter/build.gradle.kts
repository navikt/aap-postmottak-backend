val jacksonVersion = "2.20.1"

plugins {
    id("aap.conventions")
}

dependencies {
    implementation(project(":kontrakt"))
    implementation(project(":flyt"))
    
    // TODO: undersøk om vi kan bruke en enklere algoritme for arbeidsdager
    implementation(libs.bekk.no.commons)

    implementation(libs.httpklient)
    implementation(libs.infrastructure)
    implementation(libs.behandlingsflyt.kontrakt)
    implementation(libs.arenaoppslag.kontrakt)

    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.jackson.serialization)

    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    implementation(libs.logback.classic)

    implementation(libs.unleash.client)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.bundles.junit)
    testImplementation(libs.mockk)
    testImplementation(project(":lib-test"))
    testImplementation(libs.ktor.server.netty)
}