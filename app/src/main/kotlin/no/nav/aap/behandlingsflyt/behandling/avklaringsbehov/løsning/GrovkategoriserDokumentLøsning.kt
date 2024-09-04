package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.GROVKATEGORISER_DOKUMENT_KODE
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.GrovkategoriserDokumentLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.komponenter.dbconnect.DBConnection


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = GROVKATEGORISER_DOKUMENT_KODE)
class GrovkategoriserDokumentLøsning(
    @JsonProperty("skalTilAap", required = true)
    val skalTilAap: Boolean,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = GROVKATEGORISER_DOKUMENT_KODE
    ) val behovstype: String = GROVKATEGORISER_DOKUMENT_KODE
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return GrovkategoriserDokumentLøser(connection).løs(kontekst, this)
    }
}


