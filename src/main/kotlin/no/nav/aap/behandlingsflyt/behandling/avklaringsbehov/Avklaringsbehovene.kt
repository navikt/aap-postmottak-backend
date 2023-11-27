package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

class Avklaringsbehovene(private val repository: AvklaringsbehovOperasjonerRepository, private val behandlingId: BehandlingId) {

    private val avklaringsbehovene: MutableList<Avklaringsbehov> = repository.hentBehovene(behandlingId).toMutableList()

    fun leggTil(funnetAvklaringsbehov: List<Definisjon>, steg: StegType) {
        funnetAvklaringsbehov.stream()
            .map { definisjon ->
                Avklaringsbehov(
                    id = Long.MAX_VALUE,
                    definisjon = definisjon,
                    funnetISteg = steg,
                    kreverToTrinn = null
                )
            }
            .forEach { this.leggTil(it) }
    }

    fun leggTil(avklaringsbehov: Avklaringsbehov) {
        val relevantBehov = avklaringsbehovene.firstOrNull { it.definisjon == avklaringsbehov.definisjon }

        if (relevantBehov != null) {
            repository.opprettAvklaringsbehovEndring(
                avklaringsbehovId = relevantBehov.id,
                status = Status.OPPRETTET,
                begrunnelse = "",
                opprettetAv = "system"
            )
            relevantBehov.reåpne()
        } else {
            repository.leggTilAvklaringsbehov(
                behandlingId = behandlingId,
                definisjon = avklaringsbehov.definisjon,
                funnetISteg = avklaringsbehov.funnetISteg
            )
            avklaringsbehovene.add(avklaringsbehov)
        }
    }

    fun løsAvklaringsbehov(definisjon: Definisjon, begrunnelse: String, endretAv: String) {
        val avklaringsbehov = avklaringsbehovene.single { it.definisjon == definisjon }
        avklaringsbehov.løs(begrunnelse, endretAv = endretAv)
        repository.opprettAvklaringsbehovEndring(
            avklaringsbehovId = avklaringsbehov.id,
            status = Status.AVSLUTTET,
            begrunnelse = begrunnelse,
            opprettetAv = endretAv
        )
    }

    fun løsAvklaringsbehov(definisjon: Definisjon, begrunnelse: String, endretAv: String, kreverToTrinn: Boolean) {
        val avklaringsbehov = avklaringsbehovene.single { it.definisjon == definisjon }
        avklaringsbehov.løs(begrunnelse = begrunnelse, endretAv = endretAv, kreverToTrinn = kreverToTrinn)
        repository.kreverToTrinn(avklaringsbehov.id, kreverToTrinn)
        repository.opprettAvklaringsbehovEndring(
            avklaringsbehovId = avklaringsbehov.id,
            status = Status.AVSLUTTET,
            begrunnelse = begrunnelse,
            opprettetAv = endretAv
        )
    }

    fun alle(): List<Avklaringsbehov> {
        return avklaringsbehovene.toList()
    }

    fun åpne(): List<Avklaringsbehov> {
        return avklaringsbehovene.filter { it.erÅpent() }.toList()
    }

    fun tilbakeførtFraBeslutter(): List<Avklaringsbehov> {
        return avklaringsbehovene.filter { it.status() == Status.SENDT_TILBAKE_FRA_BESLUTTER }.toList()
    }

    fun hentBehovForDefinisjon(definisjon: Definisjon): Avklaringsbehov? {
        return avklaringsbehovene.filter { it.definisjon == definisjon }.singleOrNull()
    }

    fun hentBehovForDefinisjon(definisjoner: List<Definisjon>): List<Avklaringsbehov> {
        return avklaringsbehovene.filter { it.definisjon in definisjoner }.toList()
    }

    fun vurderTotrinn(definisjon: Definisjon, godkjent: Boolean, begrunnelse: String, vurdertAv: String) {
        val avklaringsbehov = avklaringsbehovene.single { it.definisjon == definisjon }
        avklaringsbehov.vurderTotrinn(begrunnelse, godkjent)
        require(avklaringsbehov.erTotrinn())
        val status = if (godkjent) {
            Status.TOTRINNS_VURDERT
        } else {
            Status.SENDT_TILBAKE_FRA_BESLUTTER
        }
        repository.opprettAvklaringsbehovEndring(avklaringsbehovId = avklaringsbehov.id, status = status, begrunnelse = begrunnelse, opprettetAv = vurdertAv)
    }

    fun avbryt(definisjon: Definisjon) {
        val avklaringsbehov = avklaringsbehovene.single { it.definisjon == definisjon }
        avklaringsbehov.avbryt()
        repository.opprettAvklaringsbehovEndring(
            avklaringsbehovId = avklaringsbehov.id,
            status = Status.AVBRUTT,
            begrunnelse = "",
            opprettetAv = "system"
        )
    }

    fun harHattAvklaringsbehov(): Boolean {
        return avklaringsbehovene.any { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
    }

    fun harHattAvklaringsbehovSomHarKrevdToTrinn() =
        avklaringsbehovene
            .filter { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
            .any { avklaringsbehov -> avklaringsbehov.erTotrinn() && !avklaringsbehov.erTotrinnsVurdert() }

    fun harIkkeForeslåttVedtak(): Boolean {
        return avklaringsbehovene
            .filter { avklaringsbehov -> avklaringsbehov.erForeslåttVedtak() }
            .none { it.status() == Status.AVSLUTTET }
    }

    fun skalTilbakeføresEtterTotrinnsVurdering(): Boolean {
        return tilbakeførtFraBeslutter().isNotEmpty()
    }

    fun reåpne(definisjon: Definisjon) {
        val avklaringsbehov = avklaringsbehovene.single { it.definisjon == definisjon }
        avklaringsbehov.reåpne()
        repository.opprettAvklaringsbehovEndring(
            avklaringsbehovId = avklaringsbehov.id,
            status = Status.OPPRETTET,
            begrunnelse = "",
            opprettetAv = "system"
        )
    }
}