package no.nav.aap.postmottak.api.faktagrunnlag.sak

import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.tilgang.plugin.kontrakt.Personreferanse
import java.time.LocalDateTime
import java.util.*

data class IdentRequest(val ident: String) : Personreferanse {
    override fun hentPersonreferanse(): String {
        return ident
    }
}

data class FinnBehandlingerResponse(
    val behandlinger: List<BehandlinginfoDTO>
)

data class BehandlinginfoDTO(
    val referanse: UUID,
    val journalPostId: String,
    val typeBehandling: TypeBehandling,
    val status: Status,
    val opprettet: LocalDateTime
)