package no.nav.aap.postmottak.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løser.AvklarTemaLøser
import no.nav.aap.postmottak.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.AVKLAR_TEMA_KODE

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_TEMA_KODE)
class AvklarTemaLøsning(
    @JsonProperty("skalTilAap", required = true)
    val skalTilAap: Boolean,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_TEMA_KODE
    ) val behovstype: String = AVKLAR_TEMA_KODE
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarTemaLøser.konstruer(repositoryProvider).løs(kontekst, this)
    }
}


