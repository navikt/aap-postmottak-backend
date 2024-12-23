package no.nav.aap.postmottak.journalpostogbehandling

import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

data class JournalpostOgBehandling(val journalpostId: JournalpostId, val behandlingId: BehandlingId) {
    override fun toString(): String {
        return "SakOgBehandling(sakId=$journalpostId, behandlingId=$behandlingId)"
    }
}