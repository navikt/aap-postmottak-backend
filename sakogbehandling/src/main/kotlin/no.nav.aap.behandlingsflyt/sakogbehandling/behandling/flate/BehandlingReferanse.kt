package no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate

import com.fasterxml.jackson.annotation.JsonValue
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.*

data class BehandlingReferanse(@PathParam("referanse") val referanse: String) {
    fun ref(): UUID {
        return UUID.fromString(referanse)
    }

    @JsonValue
    override fun toString(): String {
        return referanse
    }
}
