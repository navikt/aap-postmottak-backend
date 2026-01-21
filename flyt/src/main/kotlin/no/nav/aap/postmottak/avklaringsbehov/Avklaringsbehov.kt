package no.nav.aap.postmottak.avklaringsbehov

import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status
import no.nav.aap.postmottak.kontrakt.steg.StegType
import java.time.LocalDate

class Avklaringsbehov(
    val id: Long,
    val definisjon: Definisjon,
    val historikk: MutableList<Endring> = mutableListOf(),
    val funnetISteg: StegType
) {
    init {
        if (historikk.isEmpty()) {
            historikk += Endring(
                status = Status.OPPRETTET, begrunnelse = "", endretAv = SYSTEMBRUKER.ident
            )
        }
    }

    internal fun reåpne(
        frist: LocalDate? = null,
        begrunnelse: String = "",
        venteårsak: ÅrsakTilSettPåVent? = null,
        bruker: Bruker = SYSTEMBRUKER
    ) {
        require(historikk.last().status.erAvsluttet())
        if (definisjon.erVentebehov()) {
            requireNotNull(frist)
            requireNotNull(venteårsak)
        }
        historikk += Endring(
            status = Status.OPPRETTET,
            begrunnelse = begrunnelse,
            grunn = venteårsak,
            frist = frist,
            endretAv = bruker.ident
        )
    }

    fun erÅpent(): Boolean {
        return status().erÅpent()
    }

    fun skalStoppeHer(stegType: StegType): Boolean {
        return definisjon.skalLøsesISteg(stegType, funnetISteg) && erÅpent()
    }

    internal fun løs(begrunnelse: String, endretAv: String) {
        historikk.add(
            Endring(
                status = Status.AVSLUTTET, begrunnelse = begrunnelse, endretAv = endretAv
            )
        )
    }

    fun avbryt() {
        historikk += Endring(
            status = Status.AVBRUTT, begrunnelse = "", endretAv = SYSTEMBRUKER.ident
        )
    }

    fun erAvsluttet(): Boolean {
        return status().erAvsluttet()
    }

    fun status(): Status {
        return historikk.maxOf { it }.status
    }

    fun begrunnelse(): String = historikk.maxOf { it }.begrunnelse
    fun grunn(): ÅrsakTilSettPåVent = requireNotNull(historikk.maxOf { it }.grunn)
    fun endretAv(): String = historikk.maxOf { it }.endretAv

    fun skalLøsesISteg(type: StegType): Boolean {
        return definisjon.skalLøsesISteg(type, funnetISteg)
    }

    fun løsesISteg(): StegType {
        if (definisjon.løsesISteg == StegType.UDEFINERT) {
            return funnetISteg
        }
        return definisjon.løsesISteg
    }

    fun erVentepunkt(): Boolean {
        return definisjon.type == Definisjon.BehovType.VENTEPUNKT
    }

    fun frist(): LocalDate {
        return requireNotNull(historikk.last { it.status == Status.OPPRETTET && it.frist != null }.frist)
        { "Prøvde å finne frist, men historikk er tom. Definisjon $definisjon. Funnet i steg $funnetISteg. ID: $id. Historikk: $historikk." }
    }

    fun fristUtløpt(): Boolean {
        return frist().isBefore(LocalDate.now()) || frist().isEqual(LocalDate.now())
    }

    override fun toString(): String {
        return "Avklaringsbehov(definisjon=$definisjon, status=${status()}, løsesISteg=${løsesISteg()})"
    }

    fun harAvsluttetStatusIHistorikken(): Boolean {
        return historikk.any { it.status == Status.AVSLUTTET }
    }

    internal fun avslutt() {
        check(historikk.any { it.status == Status.AVSLUTTET }) {
            "Et steg burde vel ha vært løst minst en gang for å kunne regnes som avsluttet?"
        }

        historikk += Endring(
            status = Status.AVSLUTTET,
            begrunnelse = "",
            endretAv = SYSTEMBRUKER.ident
        )
    }
}