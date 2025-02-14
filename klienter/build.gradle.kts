val komponenterVersjon = "1.0.129"
val ktorVersion = "3.1.0"
val tilgangVersjon = "0.0.94"
val jacksonVersion = "2.18.2"
val junitVersion = "5.11.3"
val behandlingsflytVersjon = "0.0.155"

plugins {
    id("postmottak.conventions")
}

dependencies {
    implementation(project(":kontrakt"))
    implementation(project(":flyt"))
    
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.behandlingsflyt:kontrakt:$behandlingsflytVersjon")
    implementation("no.nav.aap.kelvin:cache:$komponenterVersjon")


    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("ch.qos.logback:logback-classic:1.5.16")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation(project(":lib-test"))
    testImplementation("io.ktor:ktor-server-netty:$ktorVersion")
}