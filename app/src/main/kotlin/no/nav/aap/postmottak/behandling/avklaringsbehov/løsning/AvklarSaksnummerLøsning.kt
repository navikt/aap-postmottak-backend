package no.nav.aap.postmottak.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.postmottak.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.behandling.avklaringsbehov.løser.AvklarSakLøser
import no.nav.aap.postmottak.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.AVKLAR_SAKSNUMMER_KODE

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_SAKSNUMMER_KODE)
class AvklarSaksnummerLøsning(
    @JsonProperty("saksnummer", required = true)
    val saksnummer: String?,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_SAKSNUMMER_KODE
    ) val behovstype: String = AVKLAR_SAKSNUMMER_KODE
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarSakLøser(connection).løs(kontekst, this)
    }
}