package no.nav.aap.postmottak.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løser.AvklarUnder18SøknadLøser
import no.nav.aap.postmottak.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.AVKLAR_UNDER18_SØKNAD_KODE
import no.nav.aap.postmottak.under18.Under18Søknadssituasjon

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_UNDER18_SØKNAD_KODE)
class AvklarUnder18SøknadLøsning(
    val situasjon: Under18Søknadssituasjon? = null,
    val søkerVilFortsette: Boolean? = null,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_UNDER18_SØKNAD_KODE
    ) val behovstype: String = AVKLAR_UNDER18_SØKNAD_KODE,
    val fylt17ÅrOg9Måneder: Boolean? = null,
) : AvklaringsbehovLøsning {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider,
        kontekst: AvklaringsbehovKontekst
    ): LøsningsResultat {
        return AvklarUnder18SøknadLøser.konstruer(repositoryProvider, gatewayProvider).løs(kontekst, this)
    }
}
