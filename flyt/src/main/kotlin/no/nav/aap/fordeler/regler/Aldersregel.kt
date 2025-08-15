package no.nav.aap.fordeler.regler

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.gateway.PersondataGateway
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Period

class Aldersregel : Regel<AldersregelInput> {
    private val log = LoggerFactory.getLogger(Aldersregel::class.java)

    companion object : RegelFactory<AldersregelInput> {
        override val erAktiv = miljøConfig(prod = true, dev = true)
        const val MIN_ALDER = 22
        const val MAX_ALDER = 59
        override fun medDataInnhenting(repositoryProvider: RepositoryProvider?, gatewayProvider: GatewayProvider?) =
            RegelMedInputgenerator(Aldersregel(), AldersregelInputGenerator(requireNotNull(gatewayProvider)))
    }

    override fun vurder(input: AldersregelInput): Boolean {
        if (input.fødselsdato == null) {
            log.info("Fant ikke fødselsdato for person i PDL - returnerer false")
            return false
        }
        val alder = Period.between(input.fødselsdato, input.nåDato).years
        return alder in MIN_ALDER..MAX_ALDER
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }

}

class AldersregelInputGenerator(private val gatewayProvider: GatewayProvider) : InputGenerator<AldersregelInput> {
    override fun generer(input: RegelInput): AldersregelInput {
        val fnr = input.person.aktivIdent().identifikator
        val fødselsdato =
            gatewayProvider.provide(PersondataGateway::class).hentFødselsdato(fnr)

        return AldersregelInput(fødselsdato, LocalDate.now())
    }
}

data class AldersregelInput(
    val fødselsdato: LocalDate?,
    val nåDato: LocalDate
)