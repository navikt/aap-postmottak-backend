package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.LocalDate

data class JournalpostPåSak(
    val journalpostId: JournalpostId,
    val journalstatus: Journalstatus?,
    val mottattDato: LocalDate?,
    val kanal: KanalFraKodeverk?,
    val tema: String?,
)

interface SaksnummerRepository: Repository {
    fun hentKelvinSaker(behandlingId: BehandlingId): List<Saksinfo>
    fun lagreKelvinSak(
        behandlingId: BehandlingId,
        saksinfo: List<Saksinfo>
    )
    fun lagreSakVurdering(behandlingId: BehandlingId, saksvurdering: Saksvurdering)
    fun hentSakVurdering(behandlingId: BehandlingId): Saksvurdering?
    fun hentSaksnummerForJournalpost(journalpostId: JournalpostId): String?
    fun hentJournalposterForSak(saksnummer: String): List<JournalpostPåSak>
    fun eksistererAvslagPåTidligereBehandling(behandlingId: BehandlingId): Boolean
}