package no.nav.aap.fordeler.regler

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.lookup.repository.RepositoryProvider

sealed interface Regel<T> {
    fun regelNavn(): String
    fun vurder(input: T): Boolean
    fun erAktiv(): Boolean
}

sealed interface InputGenerator<T> {
    fun generer(input: RegelInput): T
}

class RegelMedInputgenerator<T>(val regel: Regel<T>, val inputGenerator: InputGenerator<T>) : Regel<RegelInput> {
    override fun regelNavn(): String = regel.regelNavn()
    override fun vurder(input: RegelInput): Boolean {
        val regelInput = inputGenerator.generer(input)
        return regel.vurder(regelInput)
    }

    override fun erAktiv() = regel.erAktiv()
}

sealed interface RegelFactory<T> {
    fun medDataInnhenting(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider
    ): RegelMedInputgenerator<T>
}

fun miljøConfig(prod: Boolean, dev: Boolean) = when (Miljø.er()) {
    MiljøKode.PROD -> prod
    MiljøKode.DEV -> dev
    else -> dev
}