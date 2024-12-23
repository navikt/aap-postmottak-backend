package no.nav.aap.postmottak.journalpostogbehandling.behandling.flate

import com.fasterxml.jackson.annotation.JsonValue
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.*

data class BehandlingReferanse(@JsonValue @PathParam("referanse") val referanse: UUID = UUID.randomUUID()) {
    override fun toString() = referanse.toString()
}
