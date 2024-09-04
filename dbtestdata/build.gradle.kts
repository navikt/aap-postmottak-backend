
val komponenterVersjon = "0.0.23"


dependencies {
    implementation(project(":dbtest"))
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation(project(":verdityper"))
}
