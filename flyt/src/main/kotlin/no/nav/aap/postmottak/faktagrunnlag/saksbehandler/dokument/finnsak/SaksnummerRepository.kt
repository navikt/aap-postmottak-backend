package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

interface SaksnummerRepository: Repository {
    fun hentSaksnumre(behandlingId: BehandlingId): List<Saksinfo>
    fun lagreSaksnummer(
        behandlingId: BehandlingId,
        saksinfo: List<Saksinfo>
    )
    fun lagreSakVurdering(behandlingId: BehandlingId, saksvurdering: Saksvurdering)
    fun hentSakVurdering(behandlingId: BehandlingId): Saksvurdering?
}