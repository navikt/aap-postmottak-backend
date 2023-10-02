package no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak

import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.FATTE_VEDTAK_KODE

@JsonTypeName(value = FATTE_VEDTAK_KODE)
class FatteVedtakLøsning(val begrunnelse: String) :
    AvklaringsbehovLøsning
