package no.nav.aap.postmottak.kontrakt.journalpost

import com.fasterxml.jackson.annotation.JsonValue
import com.papsign.ktor.openapigen.annotations.parameters.PathParam

public data class JournalpostId(@JsonValue @param:PathParam("referanse") val referanse: Long) {
    override fun toString(): String {
        return referanse.toString()
    }
}
