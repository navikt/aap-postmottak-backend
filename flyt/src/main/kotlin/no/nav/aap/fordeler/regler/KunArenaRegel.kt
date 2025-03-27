package no.nav.aap.fordeler.regler

import no.nav.aap.komponenter.dbconnect.DBConnection

class KunArenaRegel : Regel<Unit> {
    companion object : RegelFactory<Unit> {
        override val erAktiv = miljøConfig(prod = false, dev = false)
        override fun medDataInnhenting(connection: DBConnection?) =
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

