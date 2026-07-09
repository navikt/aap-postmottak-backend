package no.nav.aap.postmottak.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.postmottak.avklaringsbehov.løser.VurderOpprettelseAvSakLøser
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.VURDER_OPPRETTELSE_AV_SAK_KODE
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.VurderOpprettelseAvSakValg

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = VURDER_OPPRETTELSE_AV_SAK_KODE)
class VurderOpprettelseAvSakLøsning(
    val valg: VurderOpprettelseAvSakValg? = null,
    val begrunnelse: String? = null,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = VURDER_OPPRETTELSE_AV_SAK_KODE
    ) val behovstype: String = VURDER_OPPRETTELSE_AV_SAK_KODE
) : AvklaringsbehovLøsning {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider,
        kontekst: AvklaringsbehovKontekst
    ): LøsningsResultat {
        return VurderOpprettelseAvSakLøser.konstruer(repositoryProvider, gatewayProvider).løs(kontekst, this)
    }
}

