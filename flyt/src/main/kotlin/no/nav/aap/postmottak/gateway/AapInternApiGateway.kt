package no.nav.aap.postmottak.gateway

import no.nav.aap.api.intern.PersonEksistererIAAPArena
import no.nav.aap.api.intern.SignifikanteSakerResponse
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import java.time.LocalDate

interface AapInternApiGateway : Gateway {
    fun harAapSakIArena(person: Person): PersonEksistererIAAPArena

    fun harSignifikantHistorikkIAAPArena(person: Person, mottattDato: LocalDate): SignifikanteSakerResponse

    /**
     * Kontekst om søkers siste Arena-sak, brukt til å gi saksbehandler grunnlag i det manuelle
     * avklaringsbehovet VURDER_OPPRETTELSE_AV_SAK.
     *
     * TODO: Implementeres når AapInternApi/arenaoppslag eksponerer dette. Returnerer `null` inntil videre.
     */
    fun hentArenaSakskontekst(person: Person, mottattDato: LocalDate): ArenaSakskontekst? = null
}

/**
 * Grunnlag om søkers siste Arena-sak med AAP-vedtak. Alle felter er nullbare fordi dataene ennå
 * ikke hentes, og fordi de uansett kan mangle for en gitt søker.
 */
data class ArenaSakskontekst(
    val arenaSaksnummer: String? = null,
    val sakStatus: String? = null,
    val periodeStatus: String? = null,
    val gjenståendeOrdinærPeriodeDager: Int? = null,
    val gjenståendeUnntaksperiodeParagraf1112: String? = null,
    val sisteAapVedtak: String? = null,
    val sisteAapVedtakFom: LocalDate? = null,
    val sisteAapVedtakTom: LocalDate? = null,
    val sisteUtbetaling: LocalDate? = null,
    val innstillingParagraf1112Lenke: String? = null,
)


