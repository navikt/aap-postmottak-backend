package no.nav.aap.fordeler.regler

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.gateway.Uføre
import no.nav.aap.postmottak.gateway.UføreRegisterGateway
import org.slf4j.LoggerFactory
import java.time.LocalDate

/**
 *  Det er bestilt endring i Arena sin måte å håndtere denne samordningen på slik at det gjøres likt som i Kelvin,
 *  men det tar tid før denne er på plass. I mellomtiden ønsker vi å unngå at slike saker kommer inn i Kelvin.
 **/
class ErIkkeGradertUføreRegel : Regel<GradertUføreRegelInput> {
    companion object : RegelFactory<GradertUføreRegelInput> {
        override val erAktiv = miljøConfig(prod = true, dev = true)
        private val log = LoggerFactory.getLogger(javaClass)

        override fun medDataInnhenting(
            repositoryProvider: RepositoryProvider?,
            gatewayProvider: GatewayProvider?
        ): RegelMedInputgenerator<GradertUføreRegelInput> {
            return RegelMedInputgenerator(
                ErIkkeGradertUføreRegel(),
                GradertUføreRegelInputGenerator(requireNotNull(gatewayProvider))
            )
        }
    }

    override fun vurder(input: GradertUføreRegelInput): Boolean {
        val relevantePerioder = input.uførePerioder.filter { it.uføregrad != null && it.uføregrad > 0 }
        if (relevantePerioder.isEmpty()) {
            return true
        } else {
            log.info("Fant periode i PESYS med gradert uføre, returnerer false")
            return false
        }
    }

    override fun regelNavn(): String = this::class.simpleName!!
}

class GradertUføreRegelInputGenerator(
    private val gatewayProvider: GatewayProvider,
) : InputGenerator<GradertUføreRegelInput> {
    override fun generer(input: RegelInput): GradertUføreRegelInput {
        val fnr = input.person.aktivIdent().identifikator
        val klient = gatewayProvider.provide(UføreRegisterGateway::class)
        val uførePerioder = klient.innhentPerioder(fnr, LocalDate.now())

        return GradertUføreRegelInput(uførePerioder)
    }
}

data class GradertUføreRegelInput(
    val uførePerioder: List<Uføre>
)