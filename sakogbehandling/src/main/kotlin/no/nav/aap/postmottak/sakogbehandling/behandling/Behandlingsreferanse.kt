package no.nav.aap.postmottak.sakogbehandling.behandling

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.*


data class Behandlingsreferanse(@PathParam("referanse") val referanse: UUID)
