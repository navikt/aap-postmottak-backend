package no.nav.aap.postmottak.gateway

import no.nav.aap.lookup.gateway.Gateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import java.time.LocalDate

interface AapInternApiGateway: Gateway {
    fun hentAapSakerForPerson(person: Person): List<SakStatus>
}

data class SakStatus(
    val sakId: String,
    val vedtakStatusKode: String,
    val periode: Periode,
    val kilde: Kilde
)

enum class Kilde {
    ARENA,
    Kelvin
}
data class Periode(val fraOgMedDato: LocalDate?, val tilOgMedDato: LocalDate?)
