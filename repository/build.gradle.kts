plugins {
    id("postmottak.conventions")
}

val komponenterVersjon = "1.0.236"
val behandlingsflytVersjon = "0.0.256"
val junitVersjon = "5.12.2"

dependencies {
    implementation(project(":flyt"))

    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:verdityper:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.behandlingsflyt:kontrakt:$behandlingsflytVersjon")
    implementation("org.flywaydb:flyway-database-postgresql:11.8.0")
    runtimeOnly("org.postgresql:postgresql:42.7.5")

    testImplementation(project(":lib-test"))
    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
    testImplementation("org.assertj:assertj-core:3.27.3")
}