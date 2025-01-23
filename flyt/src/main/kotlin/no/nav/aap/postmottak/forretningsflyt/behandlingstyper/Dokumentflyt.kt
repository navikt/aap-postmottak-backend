package no.nav.aap.postmottak.forretningsflyt.behandlingstyper

import no.nav.aap.postmottak.flyt.BehandlingFlyt
import no.nav.aap.postmottak.flyt.BehandlingFlytBuilder
import no.nav.aap.postmottak.flyt.BehandlingType
import no.nav.aap.postmottak.forretningsflyt.steg.AvsluttBehandlingSteg
import no.nav.aap.postmottak.forretningsflyt.steg.DigitaliserDokumentSteg
import no.nav.aap.postmottak.forretningsflyt.steg.KategoriserDokumentSteg
import no.nav.aap.postmottak.forretningsflyt.steg.OverleverTilFagsystemSteg
import no.nav.aap.postmottak.forretningsflyt.steg.StartBehandlingSteg

object Dokumentflyt: BehandlingType {
    override fun flyt(): BehandlingFlyt = BehandlingFlytBuilder()
        .medSteg(steg = StartBehandlingSteg)
        .medSteg(steg = KategoriserDokumentSteg)
        .medSteg(steg = DigitaliserDokumentSteg)
        .medSteg(steg = OverleverTilFagsystemSteg)
        .medSteg(steg = AvsluttBehandlingSteg)
        .build()
}