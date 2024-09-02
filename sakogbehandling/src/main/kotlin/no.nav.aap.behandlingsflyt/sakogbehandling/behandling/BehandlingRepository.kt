package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

interface BehandlingRepository {

    fun opprettBehandling(journalpostId: JournalpostId): Behandling

    fun lagreGrovvurdeingVurdering(behandlingId: BehandlingId, vurdering: Boolean)

    fun lagreKategoriseringVurdering(behandlingId: BehandlingId, kategori: Brevkode)

    fun lagreStrukturertDokument(behandlingId: BehandlingId, strukturertDokument: String)

    fun hent(behandlingId: BehandlingId): Behandling

    fun hent(referanse: BehandlingReferanse): Behandling
}

