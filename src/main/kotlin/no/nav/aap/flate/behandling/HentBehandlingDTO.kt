package no.nav.aap.flate.behandling

import com.papsign.ktor.openapigen.annotations.parameters.PathParam

data class HentBehandlingDTO(@PathParam("referanse") val referanse: String) {

}
