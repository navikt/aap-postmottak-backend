package no.nav.aap.postmottak.fordeler.regler

sealed interface Regel<T> {
    fun regelNavn(): String
    fun vurder(input: T): Boolean
}

sealed interface InputGenerator<T> {
    fun generer(input: RegelInput): T
}

class RegelMedInputgenerator<T>(val regel: Regel<T>, val inputGenerator: InputGenerator<T>): Regel<RegelInput> {
    override fun regelNavn(): String = regel.regelNavn()
    override fun vurder(input: RegelInput): Boolean {
        val regelInput = inputGenerator.generer(input)
        return regel.vurder(regelInput)
    }
}

sealed interface RegelFactory<T> {
    val erAktiv: Boolean
    fun medDataInnhenting(): RegelMedInputgenerator<T>
}