package no.nav.aap.postmottak.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løser.AvklarUnder18SamtykkeLøser
import no.nav.aap.postmottak.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.AVKLAR_UNDER18_SAMTYKKE_KODE

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_UNDER18_SAMTYKKE_KODE)
class AvklarUnder18SamtykkeLøsning(
    @param:JsonProperty("beggeForeldreSamtykket", required = true)
    val beggeForeldreSamtykket: Boolean,
    @param:JsonProperty("samtykkeJournalpostIder")
    val samtykkeJournalpostIder: List<Long> = emptyList(),
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_UNDER18_SAMTYKKE_KODE
    ) val behovstype: String = AVKLAR_UNDER18_SAMTYKKE_KODE,
) : AvklaringsbehovLøsning {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider,
        kontekst: AvklaringsbehovKontekst
    ): LøsningsResultat {
        return AvklarUnder18SamtykkeLøser.konstruer(repositoryProvider, gatewayProvider).løs(kontekst, this)
    }
}
