package no.nav.aap.postmottak.fordeler.regler

import no.nav.aap.verdityper.Brevkoder


data class ReisestønadRegelInput(
    val brevkode: Brevkoder
)


class ReisestønadRegel : Regel<ReisestønadRegelInput> {
    companion object : RegelFactory<ReisestønadRegelInput> {
        override val erAktiv = true
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
        val brevkode = try { input.brevkode.let(Brevkoder::valueOf) } catch (_: IllegalArgumentException) { Brevkoder.ANNEN }
        return ReisestønadRegelInput(brevkode)
    }
}
