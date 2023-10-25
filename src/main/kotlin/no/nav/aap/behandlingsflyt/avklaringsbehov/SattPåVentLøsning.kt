package no.nav.aap.behandlingsflyt.avklaringsbehov

import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.MANUELT_SATT_PÅ_VENT_KODE

@JsonTypeName(value = MANUELT_SATT_PÅ_VENT_KODE)
class SattPåVentLøsning: AvklaringsbehovLøsning