package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.KATEGORISER_DOKUMENT_KODE
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.KategoriserDokumentLøser
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.flate.KategoriavklaringDto
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = KATEGORISER_DOKUMENT_KODE)
class KategoriserDokumentLøsning(
    @JsonProperty("bistandsVurdering", required = true)
    val kategori: KategoriavklaringDto,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = KATEGORISER_DOKUMENT_KODE
    ) val behovstype: String = KATEGORISER_DOKUMENT_KODE
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return KategoriserDokumentLøser(connection).løs(kontekst, this)
    }
}


