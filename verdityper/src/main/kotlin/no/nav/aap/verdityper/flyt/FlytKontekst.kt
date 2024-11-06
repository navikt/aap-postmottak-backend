package no.nav.aap.verdityper.flyt

import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

data class FlytKontekst(
    val journalpostId: JournalpostId,
    val behandlingId: BehandlingId,
    val behandlingType: TypeBehandling
)