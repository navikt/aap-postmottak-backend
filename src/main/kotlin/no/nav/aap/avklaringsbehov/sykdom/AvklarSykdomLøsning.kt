package no.nav.aap.avklaringsbehov.sykdom

import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.avklaringsbehov.AvklaringsbehovLøsning
import no.nav.aap.domene.behandling.avklaringsbehov.AVKLAR_SYKDOM_KODE

@JsonTypeName(value = AVKLAR_SYKDOM_KODE)
class AvklarSykdomLøsning(begrunnelse: String, endretAv: String, val vurdertePerioder: Set<PeriodeMedUtfall>) :
    AvklaringsbehovLøsning(begrunnelse, endretAv) {

}
