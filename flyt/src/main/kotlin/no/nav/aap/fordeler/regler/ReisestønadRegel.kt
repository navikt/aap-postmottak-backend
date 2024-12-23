package no.nav.aap.fordeler.regler

import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder

data class ReisestønadRegelInput(
    val brevkode: Brevkoder
)

class ReisestønadRegel : Regel<ReisestønadRegelInput> {
    companion object : RegelFactory<ReisestønadRegelInput> {
        override val erAktiv = miljøConfig(prod = true, dev = true)
        override fun medDataInnhenting() =
            RegelMedInputgenerator(ReisestønadRegel(), ReisestønadRegelInputGenerator())
    }

    override fun vurder(input: ReisestønadRegelInput): Boolean {
        return !listOf(
            Brevkoder.SØKNAD_OM_REISESTØNAD,
            Brevkoder.SØKNAD_OM_REISESTØNAD_ETTERSENDELSE
        ).contains(input.brevkode)
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}

class ReisestønadRegelInputGenerator : InputGenerator<ReisestønadRegelInput> {
    override fun generer(input: RegelInput): ReisestønadRegelInput {
        val brevkode = input.brevkode.let(Brevkoder::fraKode)
        return ReisestønadRegelInput(brevkode)
    }
}
