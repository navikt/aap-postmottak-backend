package no.nav.aap.postmottak.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "behovstype", visible = true)
sealed interface AvklaringsbehovLøsning {
    fun definisjon(): Definisjon {
        if (this.javaClass.isAnnotationPresent(JsonTypeName::class.java)) {
            return Definisjon.entries.first {
                it.kode == AvklaringsbehovKode.valueOf(
                    this.javaClass.getDeclaredAnnotation(
                        JsonTypeName::class.java
                    ).value
                )
            }
        }
        throw IllegalStateException("Utvikler-feil:" + this.javaClass.getSimpleName() + " er uten JsonTypeName annotation.")
    }

    fun løs(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider,
        kontekst: AvklaringsbehovKontekst
    ): LøsningsResultat
}

fun utledSubtypes(): List<Class<*>> {
    return AvklaringsbehovLøsning::class.sealedSubclasses.map { it.java }.toList()
}