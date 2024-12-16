package no.nav.aap.postmottak.klient.behandlingsflyt

import java.time.LocalDate

data class FinnEllerOpprettSak(
    val ident: String,
    val søknadsdato: LocalDate,
)

data class FinnSaker(val ident: String)