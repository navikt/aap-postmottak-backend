package no.nav.aap.postmottak.behandling.avklaringsbehov.løsning

import no.nav.aap.postmottak.behandling.avklaringsbehov.løser.KategoriserDokumentLøser
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.postmottak.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.KATEGORISER_DOKUMENT_KODE


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = KATEGORISER_DOKUMENT_KODE)
class KategoriserDokumentLøsning(
    @JsonProperty("dokumentkategori", required = true)
    val kategori: Brevkode,
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


