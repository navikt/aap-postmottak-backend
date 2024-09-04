package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.AvklarTemaSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.DigitaliserDokumentSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FinnSakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.KategoriserDokumentSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.OverleverTilFagsystemSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.StartBehandlingSteg

object Dokumentflyt: BehandlingType {
    override fun flyt(): BehandlingFlyt = BehandlingFlytBuilder()
        .medSteg(steg = StartBehandlingSteg)
        .medSteg(steg = AvklarTemaSteg)
        .medSteg(steg = FinnSakSteg)
        .medSteg(steg = KategoriserDokumentSteg)
        .medSteg(steg = DigitaliserDokumentSteg)
        .medSteg(steg = OverleverTilFagsystemSteg)
        .build()
}