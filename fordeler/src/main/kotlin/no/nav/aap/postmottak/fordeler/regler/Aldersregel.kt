package no.nav.aap.postmottak.fordeler.regler

import no.nav.aap.postmottak.klient.pdl.PdlGraphQLClient
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Period

private val log = LoggerFactory.getLogger(Aldersregel::class.java)

class Aldersregel : Regel<AldersregelInput> {
    companion object : RegelFactory<AldersregelInput> {
        override val erAktiv = true
        override fun medDataInnhenting() =
            RegelMedInputgenerator(Aldersregel(), AldersregelInputGenerator())
    }

    override fun vurder(input: AldersregelInput): Boolean {
        // TODO: Fjern logging av persondata
        val res = Period.between(input.fødselsdato, input.nåDato).years < 65
        log.info("Vurderer Aldersregel for person født ${input.fødselsdato} mot ${input.nåDato}: $res")
        return res
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }

}

class AldersregelInputGenerator : InputGenerator<AldersregelInput> {
    override fun generer(input: RegelInput): AldersregelInput {
        val fødselsdato =
            PdlGraphQLClient.withClientCredentialsRestClient()
                .hentPerson(input.fnr)?.foedselsdato?.first()?.foedselsdato
                ?: throw RuntimeException("Fant ikke fødselsdato for person") // TODO: Håndter denne
        return AldersregelInput(fødselsdato, LocalDate.now())
    }
}

data class AldersregelInput(
    val fødselsdato: LocalDate,
    val nåDato: LocalDate
)