val jacksonVersion = "2.20.1"

plugins {
    id("aap.conventions")
}

dependencies {
    implementation(project(":kontrakt"))
    implementation(project(":flyt"))
    
    // TODO: undersøk om vi kan bruke en enklere algoritme for arbeidsdager
    implementation(libs.bekkNoCommons)

    implementation(libs.httpklient)
    implementation(libs.infrastructure)
    implementation(libs.behandlingsflytKontrakt)
    implementation(libs.apiInternKontrakt)
    implementation(libs.arenaoppslagKontrakt)

    implementation(libs.ktorClientAuth)
    implementation(libs.ktorClientCio)
    implementation(libs.ktorClientContentNegotiation)
    implementation(libs.ktorClientJackson)
    implementation(libs.ktorClientJacksonSerialization)
    implementation(libs.ktorClientLogging)

    implementation(libs.jacksonDatabind)
    implementation(libs.jacksonDatatypeJsr310)

    implementation(libs.logbackClassic)

    implementation(libs.unleashClient)
    implementation(libs.kotlinxCoroutinesCore)

    testImplementation(libs.bundles.junit)
    testImplementation(libs.mockk)
    testImplementation(project(":lib-test"))
    testImplementation(libs.ktorServerNetty)
}