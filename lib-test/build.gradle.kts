plugins {
    id("postmottak.conventions")
}

dependencies {
    implementation(project(":flyt"))
    implementation(project(":klienter"))
    implementation(project(":repository"))

    implementation(libs.tilgangKontrakt)
    implementation(libs.behandlingsflytKontrakt)
    implementation(libs.httpklient)
    implementation(libs.dbconnect)
    implementation(libs.dbtest)
    implementation(libs.ktorServerContentNegotation)
    implementation(libs.ktorServerNetty)
    constraints {
        implementation("io.netty:netty-common:4.2.4.Final")
    }
    implementation(libs.ktorServerCors)
    implementation(libs.ktorServerStatusPages)
    implementation(libs.ktorSerializationJackson)

    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2")
    implementation(libs.bundles.junit)

    implementation("com.nimbusds:nimbus-jose-jwt:10.4.1")
}