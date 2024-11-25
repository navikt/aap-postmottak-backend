package no.nav.aap.postmottak.fordeler.regler

import no.nav.aap.postmottak.klient.pdl.GeografiskTilknytning
import no.nav.aap.postmottak.klient.pdl.GeografiskTilknytningType
import no.nav.aap.postmottak.klient.pdl.PdlGraphQLClient

class GeografiskTilknytningRegel : Regel<GeografiskTilknytningRegelInput> {
    companion object : RegelFactory<GeografiskTilknytningRegelInput> {
        override val erAktiv = true
        override fun medDataInnhenting() =
            RegelMedInputgenerator(GeografiskTilknytningRegel(), GeografiskTilknytningRegelInputGenerator())
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

class GeografiskTilknytningRegelInputGenerator : InputGenerator<GeografiskTilknytningRegelInput> {
    private val godkjenteGeografiskeTilknytninger = emptyList<GeografiskTilknytning>()

    override fun generer(input: RegelInput): GeografiskTilknytningRegelInput {
        val geografiskTilknytning =
            PdlGraphQLClient.withClientCredentialsRestClient().hentGeografiskTilknytning(input.fnr)
        return GeografiskTilknytningRegelInput(geografiskTilknytning, godkjenteGeografiskeTilknytninger)
    }
}


data class GeografiskTilknytningRegelInput(
    val geografiskTilknytning: GeografiskTilknytning?,
    val godkjenteGeografiskeTilknytninger: List<GeografiskTilknytning>
)