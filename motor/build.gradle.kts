
val komponenterVersjon = "0.0.46"
val ktorVersion = "2.3.12"

dependencies {
    implementation(project(":verdityper"))
    implementation(project(":dbflyway"))
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("no.nav:ktor-openapi-generator:1.0.18")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("io.ktor:ktor-http-jvm:$ktorVersion")

    testImplementation(project(":dbtestdata"))
    testImplementation(project(":dbtest"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.testcontainers:postgresql:1.20.1")
    testImplementation(kotlin("test"))
}