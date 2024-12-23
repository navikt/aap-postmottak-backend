package no.nav.aap.postmottak.journalpostogbehandling.flyt

import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

data class FlytKontekst(
    val journalpostId: JournalpostId,
    val behandlingId: BehandlingId,
    val behandlingType: TypeBehandling
)