package no.nav.aap.postmottak.under18

import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.Mottakshendelse
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

/**
 * Utleder samtykkekandidater fra mottakshistorikken i `INNKOMMENDE_JOURNALPOST`: journalposter på
 * søkeren som ble mottatt etter at samtykkeavklaringen startet i behandlingen.
 */
class Under18Samtykkekandidater(
    private val journalpostRepository: JournalpostRepository,
    private val innkommendeJournalpostRepository: InnkommendeJournalpostRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
) {
    fun hent(behandling: Behandling): List<Mottakshendelse> {
        val samtykkeavklaringStartet = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
            .hentBehovForDefinisjon(Definisjon.AVKLAR_UNDER18_SAMTYKKE)
            ?.historikk?.minOf { it.tidsstempel }
            ?: return emptyList()
        val person = checkNotNull(journalpostRepository.hentHvisEksisterer(behandling.journalpostId)?.person) {
            "Fant ikke person for hovedjournalposten til behandling ${behandling.id}"
        }

        return innkommendeJournalpostRepository.finnMottakshistorikk(person.identer())
            .filter { it.journalpostId != behandling.journalpostId }
            .filter { it.mottattTid >= samtykkeavklaringStartet }
            .sortedBy { it.mottattTid }
    }

    companion object {
        fun konstruer(repositoryProvider: RepositoryProvider) = Under18Samtykkekandidater(
            repositoryProvider.provide(),
            repositoryProvider.provide(),
            repositoryProvider.provide(),
        )
    }
}
