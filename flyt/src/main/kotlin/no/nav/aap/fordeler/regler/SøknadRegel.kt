package no.nav.aap.fordeler.regler

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder

data class SøknadRegelInput(
    val brevkode: Brevkoder
)

/** Denne aktiveres dersom vi ønsker at Kelvin-sak kun skal opprettes for søknad.
 *  Dvs. at eventuelle andre dokumenter som sendes inn før søknad vil havne i Gosys.
 *  Påfølgende dokumenter vil havne i Kelvin, ettersom KelvinSakRegel overstyrer.
 */
class SøknadRegel : Regel<SøknadRegelInput> {
    companion object : RegelFactory<SøknadRegelInput> {
        override fun medDataInnhenting(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) =
            RegelMedInputgenerator(SøknadRegel(), SøknadRegelInputGenerator())
    }

    override fun erAktiv() =  miljøConfig(prod = true, dev = false)

    override fun vurder(input: SøknadRegelInput): Boolean {
        return listOf(
            Brevkoder.SØKNAD,
        ).contains(input.brevkode)
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}

class SøknadRegelInputGenerator : InputGenerator<SøknadRegelInput> {
    override fun generer(input: RegelInput): SøknadRegelInput {
        val brevkode = input.brevkode.let(Brevkoder::fraKode)
        return SøknadRegelInput(brevkode)
    }
}
