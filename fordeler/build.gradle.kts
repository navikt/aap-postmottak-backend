val junitVersion = "5.10.3"
val komponenterVersjon = "0.0.92"

plugins {
    id("postmottak.conventions")
}

dependencies {
    implementation(project(":klienter"))
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation(project(":lib-test"))
}