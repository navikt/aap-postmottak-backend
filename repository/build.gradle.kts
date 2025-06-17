plugins {
    id("postmottak.conventions")
}

val komponenterVersjon = "1.0.269"
val behandlingsflytVersjon = "0.0.307"
val junitVersjon = "5.13.1"

dependencies {
    implementation(project(":flyt"))

    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:verdityper:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.behandlingsflyt:kontrakt:$behandlingsflytVersjon")
    implementation("org.flywaydb:flyway-database-postgresql:11.9.1")
    runtimeOnly("org.postgresql:postgresql:42.7.7")

    testImplementation(project(":lib-test"))
    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
    testImplementation("org.assertj:assertj-core:3.27.3")
}