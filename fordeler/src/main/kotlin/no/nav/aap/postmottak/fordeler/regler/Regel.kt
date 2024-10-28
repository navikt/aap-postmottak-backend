package no.nav.aap.postmottak.fordeler.regler

sealed interface Regel<T> {
    fun vurder(input: T): Boolean
}

sealed interface InputGenerator<T> {
    suspend fun generer(input: RegelInput): T
}

class RegelMedInputgenerator<T>(val regel: Regel<T>, val inputGenerator: InputGenerator<T>) {
    suspend fun vurder(input: RegelInput): Boolean {
        val regelInput = inputGenerator.generer(input)
        return regel.vurder(regelInput)
    }
}