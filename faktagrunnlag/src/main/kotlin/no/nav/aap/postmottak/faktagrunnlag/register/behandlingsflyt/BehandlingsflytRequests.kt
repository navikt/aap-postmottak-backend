package no.nav.aap.postmottak.faktagrunnlag.register.behandlingsflyt

import com.fasterxml.jackson.annotation.JsonRawValue
import java.time.LocalDate

data class FinnEllerOpprettSak(
    val ident: String,
    val søknadsdato: LocalDate,
)

data class FinnSaker(val ident: String)

data class SendSøknad(
    val saksnummer: String,
    val journalpostId: String,
    @JsonRawValue
    val søknad: Any
)
