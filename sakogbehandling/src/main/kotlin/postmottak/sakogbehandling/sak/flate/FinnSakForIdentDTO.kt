package no.nav.aap.postmottak.sakogbehandling.sak.flate

import com.fasterxml.jackson.annotation.JsonProperty

data class FinnSakForIdentDTO(@JsonProperty(value = "ident", required = true) val ident: String)