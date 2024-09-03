package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

interface VurderingRepository {
    fun lagreGrovvurdeing(behandlingId: BehandlingId, vurdering: Boolean)
    fun lagreKategoriseringVurdering(behandlingId: BehandlingId, kategori: Brevkode)
    fun lagreStrukturertDokument(behandlingId: BehandlingId, strukturertDokument: String)
}

interface BehandlingRepository {

    fun opprettBehandling(journalpostId: JournalpostId): Behandling

    fun hent(behandlingId: BehandlingId): Behandling

    fun hent(journalpostId: JournalpostId): Behandling
}

