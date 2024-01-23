package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.papsign.ktor.openapigen.annotations.parameters.PathParam

class HentSakDTO(@PathParam("saksnummer") val saksnummer: String)
