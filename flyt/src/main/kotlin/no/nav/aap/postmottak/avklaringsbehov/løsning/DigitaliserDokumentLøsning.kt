package no.nav.aap.postmottak.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løser.DigitaliserDokumentLøser
import no.nav.aap.postmottak.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.DIGITALISER_DOKUMENT_KODE
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = DIGITALISER_DOKUMENT_KODE)
class DigitaliserDokumentLøsning(
    @JsonProperty(required = true)
    val kategori: InnsendingType,
    @JsonProperty(required = true)
    val strukturertDokument: String?,
    @JsonProperty(required = true)
    val søknadsdato: LocalDate?,
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


