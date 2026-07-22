package no.nav.aap.postmottak.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løser.AvklarFordelingLøser
import no.nav.aap.postmottak.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.AVKLAR_FORDELING_KODE

/**
 * Saksbehandler sitt valg av system ved manuell vurdering av fordeling.
 * BEGGE er foreløpig ikke støttet – valget avvises ved løsing (rutingen implementeres senere).
 */
enum class FordelingSystemValg {
    ARENA, KELVIN, BEGGE
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_FORDELING_KODE)
class AvklarFordelingLøsning(
    @param:JsonProperty("valgtSystem", required = true)
    val valgtSystem: FordelingSystemValg,
    @param:JsonProperty("kommentar")
    val kommentar: String? = null,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_FORDELING_KODE
    ) val behovstype: String = AVKLAR_FORDELING_KODE
) : AvklaringsbehovLøsning {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider,
        kontekst: AvklaringsbehovKontekst
    ): LøsningsResultat {
        return AvklarFordelingLøser.konstruer(repositoryProvider, gatewayProvider).løs(kontekst, this)
    }
}

