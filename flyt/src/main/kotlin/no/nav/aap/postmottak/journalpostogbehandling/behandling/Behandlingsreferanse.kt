package no.nav.aap.postmottak.journalpostogbehandling.behandling

import com.fasterxml.jackson.annotation.JsonValue
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.*

open class Behandlingsreferanse(@JsonValue open val referanse: UUID)

data class BehandlingsreferansePathParam(
    @param:PathParam("referanse") override val referanse: UUID
): Behandlingsreferanse(referanse)