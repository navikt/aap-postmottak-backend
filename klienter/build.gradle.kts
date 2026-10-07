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

    implementation(kelvinLibs.ktor.client.cio)
    implementation(kelvinLibs.ktor.client.content.negotiation)
    implementation(kelvinLibs.ktor.serialization.jackson)

    implementation(kelvinLibs.jackson.databind)
    implementation(kelvinLibs.jackson.datatype.jsr310)

    implementation(kelvinLibs.logback.classic)

    implementation(kelvinLibs.unleash.client.java)
    implementation(kelvinLibs.coroutines.core)

    testImplementation(kelvinLibs.bundles.junit)
    testImplementation(kelvinLibs.mockk)
    testImplementation(project(":lib-test"))
    testImplementation(kelvinLibs.ktor.server.netty)
}