plugins {
    id("postmottak.conventions")
}

val junitVersjon = "5.13.1"
val ktorVersion = "3.2.0"
val komponenterVersjon = "1.0.269"
val tilgangVersjon = "1.0.80"
val behandlingsflytVersjon = "0.0.307"

dependencies {
    api(project(":flyt"))
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor-api:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:verdityper:$komponenterVersjon")
    api("no.nav.aap.tilgang:plugin:$tilgangVersjon")
    api("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")
    api("no.nav.aap.behandlingsflyt:kontrakt:$behandlingsflytVersjon")
    compileOnly("io.ktor:ktor-http-jvm:$ktorVersion")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersjon")
    testImplementation("org.assertj:assertj-core:3.27.3")
}
