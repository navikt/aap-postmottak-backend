package no.nav.aap.postmottak.test.inmemoryrepo

import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.util.concurrent.ConcurrentHashMap

object InMemoryJournalpostRepository : JournalpostRepository {
    private val journalposter = ConcurrentHashMap<JournalpostId, Journalpost>()

    override fun lagre(journalpost: Journalpost) {
        journalposter[journalpost.journalpostId] = journalpost
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId) =
        journalposter[InMemoryBehandlingRepository.hent(behandlingId).journalpostId]

    override fun hentHvisEksisterer(behandlingsreferanse: Behandlingsreferanse) =
        journalposter[InMemoryBehandlingRepository.hent(behandlingsreferanse).journalpostId]

    override fun hentHvisEksisterer(journalpostId: JournalpostId) = journalposter[journalpostId]
}
