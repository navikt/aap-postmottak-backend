package no.nav.aap.fordeler.regler

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder

data class ErIkkeAnkeRegelInput(
    val brevkode: Brevkoder
)

class ErIkkeAnkeRegel : Regel<ErIkkeAnkeRegelInput> {
    companion object : RegelFactory<ErIkkeAnkeRegelInput> {
        // Oppdater RegelResultat dersom denne deaktiveres
        override val erAktiv = miljøConfig(prod = true, dev = true)
        override fun medDataInnhenting(repositoryProvider: RepositoryProvider?, gatewayProvider: GatewayProvider?) =
            RegelMedInputgenerator(ErIkkeAnkeRegel(), ErIkkeAnkeRegelInputGenerator())
    }

    override fun vurder(input: ErIkkeAnkeRegelInput): Boolean {
        return !listOf(
            Brevkoder.ANKE,
            Brevkoder.ANKE_ETTERSENDELSE
        ).contains(input.brevkode)
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}

class ErIkkeAnkeRegelInputGenerator : InputGenerator<ErIkkeAnkeRegelInput> {
    override fun generer(input: RegelInput): ErIkkeAnkeRegelInput {
        val brevkode = input.brevkode.let(Brevkoder::fraKode)
        return ErIkkeAnkeRegelInput(brevkode)
    }
}
