package no.nav.aap.postmottak.fordeler.regler

import no.nav.aap.postmottak.klient.AapInternApiClient
import no.nav.aap.postmottak.klient.ArenaSak

class ArenaSakRegel : Regel<ArenaSakRegelInput> {
    companion object : RegelFactory<ArenaSakRegelInput> {
        override val erAktiv = false
        override fun medDataInnhenting() =
            RegelMedInputgenerator(ArenaSakRegel(), ArenaSakRegelInputGenerator())
    }

    override fun vurder(input: ArenaSakRegelInput): Boolean {
        return input.saker.isEmpty()
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}

class ArenaSakRegelInputGenerator : InputGenerator<ArenaSakRegelInput> {
    override fun generer(input: RegelInput): ArenaSakRegelInput {
        val saker = AapInternApiClient().hentArenaSakerForIdent(input.fnr)
        return ArenaSakRegelInput(saker)
    }
}

data class ArenaSakRegelInput(
    val saker: List<ArenaSak>
)