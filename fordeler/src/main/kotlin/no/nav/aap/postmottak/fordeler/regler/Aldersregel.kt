package no.nav.aap.postmottak.fordeler.regler

import no.nav.aap.postmottak.klient.pdl.PdlGraphQLClient
import java.time.LocalDate
import java.time.Period

class Aldersregel : Regel<AldersregelInput> {
    companion object {
        fun medDataInnhenting() =
            RegelMedInputgenerator(Aldersregel(), AldersregelInputGenerator())
    }

    override fun vurder(input: AldersregelInput): Boolean {
        return Period.between(input.fødselsdato, input.nåDato).years < 65
    }
}

class AldersregelInputGenerator : InputGenerator<AldersregelInput> {
    override fun generer(input: RegelInput): AldersregelInput {
        val fødselsdato =
            PdlGraphQLClient.withClientCredentialsRestClient().hentPerson(input.fnr)?.foedselsdato?.foedselsdato
                ?: throw RuntimeException("Fant ikke fødselsdato for person") // TODO: Håndter denne
        return AldersregelInput(fødselsdato, LocalDate.now())
    }
}

data class AldersregelInput(
    val fødselsdato: LocalDate,
    val nåDato: LocalDate
)