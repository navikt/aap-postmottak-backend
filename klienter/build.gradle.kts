val komponenterVersjon = "0.0.92"
val ktorVersion = "3.0.0"
val tilgangVersjon = "0.0.18"
val jacksonVersion = "2.17.2"
val junitVersion = "5.10.3"

plugins {
    id("postmottak.conventions")
}

dependencies {
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")

    implementation(project(":kontrakt"))

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("ch.qos.logback:logback-classic:1.5.8")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation(project(":lib-test"))
}