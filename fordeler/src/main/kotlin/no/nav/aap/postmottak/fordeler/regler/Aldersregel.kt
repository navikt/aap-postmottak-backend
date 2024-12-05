package no.nav.aap.postmottak.fordeler.regler

import no.nav.aap.postmottak.klient.pdl.PdlGraphqlKlient
import java.time.LocalDate
import java.time.Period

class Aldersregel : Regel<AldersregelInput> {
    companion object : RegelFactory<AldersregelInput> {
        override val erAktiv = true
        const val MIN_ALDER = 18
        const val MAX_ALDER = 59
        override fun medDataInnhenting() =
            RegelMedInputgenerator(Aldersregel(), AldersregelInputGenerator())
    }

    override fun vurder(input: AldersregelInput): Boolean {
        val alder = Period.between(input.fødselsdato, input.nåDato).years
        return alder in MIN_ALDER..MAX_ALDER
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }

}

class AldersregelInputGenerator : InputGenerator<AldersregelInput> {
    override fun generer(input: RegelInput): AldersregelInput {
        val fnr = input.person.aktivIdent().identifikator
        val fødselsdato =
            PdlGraphqlKlient.withClientCredentialsRestClient()
                .hentPerson(fnr)?.foedselsdato?.first { !it.metadata.historisk }?.foedselsdato
                ?: throw RuntimeException("Fant ikke fødselsdato for person") // TODO: Håndter denne
        return AldersregelInput(fødselsdato, LocalDate.now())
    }
}

data class AldersregelInput(
    val fødselsdato: LocalDate,
    val nåDato: LocalDate
)