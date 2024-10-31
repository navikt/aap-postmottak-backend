package no.nav.aap.postmottak.sakogbehandling.behandling

import com.fasterxml.jackson.annotation.JsonValue
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.*


data class Behandlingsreferanse(@JsonValue @PathParam("referanse") val referanse: UUID)
