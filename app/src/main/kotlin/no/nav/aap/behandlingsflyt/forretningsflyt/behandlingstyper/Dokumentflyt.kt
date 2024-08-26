package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.flyt.steg.KategoriserDokumentSteg

object Dokumentflyt: BehandlingType {
    override fun flyt(): BehandlingFlyt = BehandlingFlytBuilder()
        .medSteg(steg = KategoriserDokumentSteg)
        .build()
}