package no.nav.aap.postmottak.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.postmottak.avklaringsbehov.løser.SattPåVentLøser
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.MANUELT_SATT_PÅ_VENT_KODE

@JsonTypeName(value = MANUELT_SATT_PÅ_VENT_KODE)
class SattPåVentLøsning(
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = MANUELT_SATT_PÅ_VENT_KODE
    ) val behovstype: String = MANUELT_SATT_PÅ_VENT_KODE
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return SattPåVentLøser(connection).løs(kontekst, this)
    }
}