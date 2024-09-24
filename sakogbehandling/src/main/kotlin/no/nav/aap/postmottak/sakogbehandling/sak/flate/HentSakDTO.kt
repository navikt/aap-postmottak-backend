package no.nav.aap.postmottak.sakogbehandling.sak.flate

import com.papsign.ktor.openapigen.annotations.parameters.PathParam

data class HentSakDTO(@PathParam("saksnummer") val saksnummer: String)
