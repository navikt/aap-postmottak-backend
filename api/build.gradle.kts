plugins {
    id("postmottak.conventions")
}

val junitVersjon = "5.11.3"
val ktorVersion = "3.0.2"
val komponenterVersjon = "1.0.98"
val tilgangVersjon = "0.0.89"
val behandlingsflytVersjon = "0.0.81"

dependencies {
    api(project(":flyt"))
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor-api:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:verdityper:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:tidslinje:$komponenterVersjon")
    api("no.nav.aap.tilgang:plugin:$tilgangVersjon")
    api("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")
    api("no.nav.aap.behandlingsflyt:kontrakt:$behandlingsflytVersjon")
    compileOnly("io.ktor:ktor-http-jvm:$ktorVersion")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersjon")
    testImplementation("org.assertj:assertj-core:3.26.3")
}
