package no.nav.aap.postmottak.klient

import no.nav.aap.postmottak.gateway.Klokke
import java.time.LocalDate

object SystemKlokke : Klokke {
    override fun dagensDato(): LocalDate = LocalDate.now()
}
