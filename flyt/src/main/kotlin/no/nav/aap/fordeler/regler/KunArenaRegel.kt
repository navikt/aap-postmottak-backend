package no.nav.aap.fordeler.regler

class KunArenaRegel : Regel<Unit> {
    companion object : RegelFactory<Unit> {
        override val erAktiv = miljøConfig(prod = true, dev = false)
        override fun medDataInnhenting() =
            RegelMedInputgenerator(KunArenaRegel(), KunArenaRegelInputGenerator())
    }

    override fun vurder(input: Unit) = false

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}

class KunArenaRegelInputGenerator : InputGenerator<Unit> {
    override fun generer(input: RegelInput) {}
}

