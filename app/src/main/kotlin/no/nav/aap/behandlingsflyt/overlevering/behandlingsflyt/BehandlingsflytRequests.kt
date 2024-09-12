package no.nav.aap.behandlingsflyt.overlevering.behandlingsflyt

import java.time.LocalDate

data class FinnEllerOpprettSak(
    val ident: String,
    val søknadsdato: LocalDate,
)

data class FinnSaker(val ident: String)

data class SendSøknad(
    val saksnummer: String,
    val journalpostId: String,
    val søknad: Any
)
