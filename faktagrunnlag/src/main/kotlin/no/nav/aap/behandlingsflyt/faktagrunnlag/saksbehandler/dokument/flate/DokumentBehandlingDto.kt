package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.flate

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling

data class DokumentBehandlingDto(
    val referanse: String,
    val journalpostId: String,
    val sakId: String?,
    val version: Long
) {
    companion object {
        fun fromBehandling(behandling: Behandling) = DokumentBehandlingDto(
            behandling.referanse.toString(),
            behandling.journalpostId.toString(),
            behandling.sakId.toString(),
            behandling.versjon
        )
    }
}