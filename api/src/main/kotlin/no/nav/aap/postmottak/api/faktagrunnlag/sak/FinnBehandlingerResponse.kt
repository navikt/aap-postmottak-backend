package no.nav.aap.postmottak.api.faktagrunnlag.sak

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import java.time.LocalDateTime
import java.util.*

data class IdentPathParam(@param:PathParam("referanse") val ident: String)

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