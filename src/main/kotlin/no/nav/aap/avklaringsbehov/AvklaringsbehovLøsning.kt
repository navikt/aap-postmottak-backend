package no.nav.aap.avklaringsbehov

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.avklaringsbehov.sykdom.AvklarSykdomLøsning
import no.nav.aap.avklaringsbehov.vedtak.FatteVedtakLøsning
import no.nav.aap.avklaringsbehov.vedtak.ForeslåVedtakLøsning
import no.nav.aap.domene.behandling.avklaringsbehov.AVKLAR_SYKDOM_KODE
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.domene.behandling.avklaringsbehov.FATTE_VEDTAK_KODE
import no.nav.aap.domene.behandling.avklaringsbehov.FORESLÅ_VEDTAK_KODE
import no.nav.aap.domene.behandling.avklaringsbehov.MANUELT_SATT_PÅ_VENT_KODE


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(SattPåVentLøsning::class, name = MANUELT_SATT_PÅ_VENT_KODE),
    JsonSubTypes.Type(AvklarSykdomLøsning::class, name = AVKLAR_SYKDOM_KODE),
    JsonSubTypes.Type(ForeslåVedtakLøsning::class, name = FORESLÅ_VEDTAK_KODE),
    JsonSubTypes.Type(FatteVedtakLøsning::class, name = FATTE_VEDTAK_KODE)
)
interface AvklaringsbehovLøsning {
    fun definisjon(): Definisjon {
        if (this.javaClass.isAnnotationPresent(JsonTypeName::class.java)) {
            return Definisjon.entries.first { it.kode == this.javaClass.getDeclaredAnnotation(JsonTypeName::class.java).value }
        }
        throw IllegalStateException("Utvikler-feil:" + this.javaClass.getSimpleName() + " er uten JsonTypeName annotation.")
    }
}