val komponenterVersjon = "0.0.19"


dependencies {
    implementation(project(":sakogbehandling"))
    implementation(project(":verdityper"))
    implementation(project(":dbflyway"))
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")

    implementation("dev.forst:ktor-openapi-generator:0.6.1")
    implementation("com.zaxxer:HikariCP:5.1.0")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")

    implementation("ch.qos.logback:logback-classic:1.5.6")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation(project(":lib-test"))

}