package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.DIGITALISER_DOKUMENT_KODE
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.KATEGORISER_DOKUMENT_KODE
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.DigitaliserDokumentLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.flate.DigitaliserDokumentDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.flate.KategoriavklaringDto


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = DIGITALISER_DOKUMENT_KODE)
class DigitaliserDokumentLøsning(
    @JsonProperty("bistandsVurdering", required = true)
    val kategori: DigitaliserDokumentDto,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = DIGITALISER_DOKUMENT_KODE
    ) val behovstype: String = DIGITALISER_DOKUMENT_KODE
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return DigitaliserDokumentLøser(connection).løs(kontekst, this)
    }
}


