val jacksonVersion = "2.20.1"

plugins {
    id("postmottak.conventions")
}

dependencies {
    implementation(project(":kontrakt"))
    implementation(project(":flyt"))
    
    // TODO: undersøk om vi kan bruke en enklere algoritme for arbeidsdager
    implementation("no.bekk.bekkopen:nocommons:0.16.0")
    
    implementation(libs.httpklient)
    implementation(libs.infrastructure)
    implementation(libs.dbconnect)
    implementation(libs.behandlingsflytKontrakt)
    implementation(libs.apiInternKontrakt)

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("ch.qos.logback:logback-classic:1.5.22")

    implementation("io.getunleash:unleash-client-java:11.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation(libs.bundles.junit)
    testImplementation(libs.mockk)
    testImplementation(project(":lib-test"))
    testImplementation(libs.ktorServerNetty)
}