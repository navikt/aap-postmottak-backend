package no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter

import com.fasterxml.jackson.annotation.JsonValue
import com.papsign.ktor.openapigen.annotations.parameters.PathParam

data class JournalpostId(@JsonValue @PathParam("referanse") val referanse: Long) {
    override fun toString(): String {
        return referanse.toString()
    }
}
