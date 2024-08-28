package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.AutomatiskKategoriseringSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.DigitaliserDokumentSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.KategoriserDokumentSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.StartBehandlingSteg

object Dokumentflyt: BehandlingType {
    override fun flyt(): BehandlingFlyt = BehandlingFlytBuilder()
        .medSteg(steg = StartBehandlingSteg)
        .medSteg(steg = AutomatiskKategoriseringSteg)
        .medSteg(steg = KategoriserDokumentSteg)
        .medSteg(steg = DigitaliserDokumentSteg)
        // TODO med steg ruting?
        .build()
}