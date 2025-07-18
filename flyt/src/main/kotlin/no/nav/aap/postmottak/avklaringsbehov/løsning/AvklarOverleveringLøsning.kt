package no.nav.aap.postmottak.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løser.AvklarOverleveringLøser
import no.nav.aap.postmottak.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.AVKLAR_OVERLEVERING_KODE

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_OVERLEVERING_KODE)
class AvklarOverleveringLøsning(
    val skalOverleveres: Boolean,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_OVERLEVERING_KODE
    ) val behovstype: String = AVKLAR_OVERLEVERING_KODE
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarOverleveringLøser.konstruer(connection).løs(kontekst, this)
    }
}