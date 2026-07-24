package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

interface SaksnummerRepository: Repository {
    fun hentKelvinSaker(behandlingId: BehandlingId): List<Saksinfo>
    fun lagreKelvinSak(
        behandlingId: BehandlingId,
        saksinfo: List<Saksinfo>
    )
    fun lagreSakVurdering(behandlingId: BehandlingId, saksvurdering: Saksvurdering)
    fun hentSakVurdering(behandlingId: BehandlingId): Saksvurdering?
    fun hentSaksnummerForJournalpost(journalpostId: JournalpostId): String?
    fun eksistererAvslagPåTidligereBehandling(behandlingId: BehandlingId): Boolean
}