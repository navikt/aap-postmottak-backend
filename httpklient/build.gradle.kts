val ktorVersion = "2.3.8"
val aapLibVersion = "4.0.3"

dependencies {
    api("io.ktor:ktor-client-core:$ktorVersion")
    api("com.github.navikt.aap-libs:ktor-auth:$aapLibVersion")

    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}
