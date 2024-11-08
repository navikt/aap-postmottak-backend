val junitVersion = "5.10.3"
val komponenterVersjon = "1.0.50"

plugins {
    id("postmottak.conventions")
}

dependencies {
    implementation(project(":klienter"))
    implementation(project(":faktagrunnlag"))
    implementation(project(":kontrakt"))
    implementation(project(":sakogbehandling"))
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor:$komponenterVersjon")
    implementation("ch.qos.logback:logback-classic:1.5.8")

    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    testImplementation(project(":lib-test"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation(project(":lib-test"))
}