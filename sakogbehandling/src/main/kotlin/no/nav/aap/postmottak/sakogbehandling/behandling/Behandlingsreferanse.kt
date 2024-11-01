package no.nav.aap.postmottak.sakogbehandling.behandling

import com.fasterxml.jackson.annotation.JsonValue
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.*


open class Behandlingsreferanse(@JsonValue open val referanse: UUID)

class BehandlingsreferansePathParam(
    @PathParam("referanse") override val referanse: UUID
): Behandlingsreferanse(referanse)