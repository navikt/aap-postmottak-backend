package no.nav.aap.postmottak.behandling.avklaringsbehov

import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status
import no.nav.aap.postmottak.kontrakt.steg.StegType
import java.time.LocalDate

class Avklaringsbehov(
    val id: Long,
    val definisjon: Definisjon,
    val historikk: MutableList<Endring> = mutableListOf(),
    val funnetISteg: StegType,
    private var kreverToTrinn: Boolean?
) {
    init {
        if (historikk.isEmpty()) {
            historikk += Endring(
                status = Status.OPPRETTET, begrunnelse = "", endretAv = SYSTEMBRUKER.ident
            )
        }
    }

    internal fun reåpne(frist: LocalDate? = null, begrunnelse: String = "", grunn: ÅrsakTilSettPåVent? = null) {
        historikk += Endring(
            status = Status.OPPRETTET,
            begrunnelse = begrunnelse,
            grunn = grunn,
            frist = frist,
            endretAv = SYSTEMBRUKER.ident
        )
    }

    fun erÅpent(): Boolean {
        return status() in setOf(
            Status.OPPRETTET, Status.SENDT_TILBAKE_FRA_BESLUTTER, Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER
        )
    }

    fun skalStoppeHer(stegType: StegType): Boolean {
        return definisjon.skalLøsesISteg(stegType, funnetISteg) && erÅpent()
    }

    internal fun løs(begrunnelse: String, endretAv: String) {
        løs(begrunnelse, endretAv, definisjon.kreverToTrinn)
    }

    internal fun løs(begrunnelse: String, endretAv: String, kreverToTrinn: Boolean) {
        if (this.kreverToTrinn != true) {
            this.kreverToTrinn = kreverToTrinn
        }
        historikk.add(
            Endring(
                status = Status.AVSLUTTET, begrunnelse = begrunnelse, endretAv = endretAv
            )
        )
    }

    fun erAvsluttet(): Boolean {
        return status() == Status.AVSLUTTET
    }

    fun status(): Status {
        return historikk.maxOf { it }.status
    }

    fun begrunnelse(): String = historikk.maxOf { it }.begrunnelse
    fun endretAv(): String = historikk.maxOf { it }.endretAv

    fun skalLøsesISteg(type: StegType): Boolean {
        return definisjon.skalLøsesISteg(type, funnetISteg)
    }

    fun harVærtSendtTilbakeFraBeslutterTidligere(): Boolean {
        return historikk.any { it.status == Status.SENDT_TILBAKE_FRA_BESLUTTER }
    }

    fun løsesISteg(): StegType {
        if (definisjon.løsesISteg == StegType.UDEFINERT) {
            return funnetISteg
        }
        return definisjon.løsesISteg
    }

    fun frist(): LocalDate {
        return requireNotNull(historikk.last { it.status == Status.OPPRETTET }.frist)
    }

    override fun toString(): String {
        return "Avklaringsbehov(definisjon=$definisjon, status=${status()}, løsesISteg=${løsesISteg()})"
    }
}