package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

interface KategoriVurderingRepository: Repository {
    fun lagreKategoriseringVurdering(behandlingId: BehandlingId, kategori: InnsendingType)
    fun hentKategoriAvklaring(behandlingId: BehandlingId): KategoriVurdering?
}
