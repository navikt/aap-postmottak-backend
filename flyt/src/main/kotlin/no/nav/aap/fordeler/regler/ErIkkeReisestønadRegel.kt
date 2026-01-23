package no.nav.aap.fordeler.regler

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder

data class ErIkkeReisestønadRegelInput(
    val brevkode: Brevkoder
)

class ErIkkeReisestønadRegel : Regel<ErIkkeReisestønadRegelInput> {
    companion object : RegelFactory<ErIkkeReisestønadRegelInput> {
        override fun medDataInnhenting(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) =
            RegelMedInputgenerator(ErIkkeReisestønadRegel(), ErIkkeReisestønadRegelInputGenerator())
    }

    // Oppdater RegelResultat dersom denne deaktiveres
    override fun erAktiv() =  miljøConfig(prod = true, dev = true)

    override fun vurder(input: ErIkkeReisestønadRegelInput): Boolean {
        return !listOf(
            Brevkoder.SØKNAD_OM_REISESTØNAD,
            Brevkoder.SØKNAD_OM_REISESTØNAD_ETTERSENDELSE
        ).contains(input.brevkode)
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}

class ErIkkeReisestønadRegelInputGenerator : InputGenerator<ErIkkeReisestønadRegelInput> {
    override fun generer(input: RegelInput): ErIkkeReisestønadRegelInput {
        val brevkode = input.brevkode.let(Brevkoder::fraKode)
        return ErIkkeReisestønadRegelInput(brevkode)
    }
}
