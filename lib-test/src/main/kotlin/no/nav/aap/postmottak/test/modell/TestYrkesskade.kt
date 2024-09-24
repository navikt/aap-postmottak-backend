package no.nav.aap.postmottak.test.modell

import java.time.LocalDate

class TestYrkesskade (
    val skadedato: LocalDate = LocalDate.now(),
    val saksreferanse: String = "1234"
)