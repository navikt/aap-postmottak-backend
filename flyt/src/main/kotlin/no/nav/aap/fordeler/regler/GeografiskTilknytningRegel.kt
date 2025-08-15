package no.nav.aap.fordeler.regler

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.gateway.GeografiskTilknytning
import no.nav.aap.postmottak.gateway.GeografiskTilknytningType
import no.nav.aap.postmottak.gateway.PersondataGateway

class GeografiskTilknytningRegel : Regel<GeografiskTilknytningRegelInput> {
    companion object : RegelFactory<GeografiskTilknytningRegelInput> {
        // Bruk enhetsregel i stedet
        override val erAktiv = miljøConfig(prod = false, dev = false)
        override fun medDataInnhenting(repositoryProvider: RepositoryProvider?, gatewayProvider: GatewayProvider?) =
            RegelMedInputgenerator(
                GeografiskTilknytningRegel(),
                GeografiskTilknytningRegelInputGenerator(requireNotNull(gatewayProvider))
            )
    }

    override fun vurder(input: GeografiskTilknytningRegelInput): Boolean {
        if (input.geografiskTilknytning == null) {
            return false
        }
        return when (input.geografiskTilknytning.gtType) {
            GeografiskTilknytningType.BYDEL -> input.godkjenteGeografiskeTilknytninger.any {
                (it.gtType == GeografiskTilknytningType.BYDEL && it.gtBydel == input.geografiskTilknytning.gtBydel)
                        || (it.gtType == GeografiskTilknytningType.KOMMUNE && it.gtKommune == parseKommune(input.geografiskTilknytning))
            }

            GeografiskTilknytningType.KOMMUNE -> input.godkjenteGeografiskeTilknytninger.any {
                it.gtType == GeografiskTilknytningType.KOMMUNE && (it.gtKommune == input.geografiskTilknytning.gtKommune)
            }

            else -> false
        }
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }

    private fun parseKommune(geografiskTilknytning: GeografiskTilknytning): String? {
        if (geografiskTilknytning.gtKommune.isNullOrEmpty()) {
            val bydel = geografiskTilknytning.gtBydel?.substring(0, 4)
            if (bydel.isNullOrEmpty()) {
                return null
            } else {
                return bydel
            }
        } else {
            return geografiskTilknytning.gtKommune
        }
    }
}

class GeografiskTilknytningRegelInputGenerator(private val gatewayProvider: GatewayProvider) :
    InputGenerator<GeografiskTilknytningRegelInput> {
    private val godkjenteGeografiskeTilknytninger = emptyList<GeografiskTilknytning>()

    override fun generer(input: RegelInput): GeografiskTilknytningRegelInput {
        val geografiskTilknytning =
            gatewayProvider.provide(PersondataGateway::class)
                .hentGeografiskTilknytning(input.person.aktivIdent().identifikator)
        return GeografiskTilknytningRegelInput(geografiskTilknytning, godkjenteGeografiskeTilknytninger)
    }
}


data class GeografiskTilknytningRegelInput(
    val geografiskTilknytning: GeografiskTilknytning?,
    val godkjenteGeografiskeTilknytninger: List<GeografiskTilknytning>
)