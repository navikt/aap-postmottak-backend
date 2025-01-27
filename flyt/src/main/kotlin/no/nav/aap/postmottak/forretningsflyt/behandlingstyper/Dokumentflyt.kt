package no.nav.aap.postmottak.forretningsflyt.behandlingstyper

import no.nav.aap.postmottak.flyt.BehandlingFlyt
import no.nav.aap.postmottak.flyt.BehandlingFlytBuilder
import no.nav.aap.postmottak.flyt.BehandlingType
import no.nav.aap.postmottak.forretningsflyt.steg.dokumentflyt.AvsluttBehandlingSteg
import no.nav.aap.postmottak.forretningsflyt.steg.dokumentflyt.DigitaliserDokumentSteg
import no.nav.aap.postmottak.forretningsflyt.steg.dokumentflyt.OverleverTilFagsystemSteg
import no.nav.aap.postmottak.forretningsflyt.steg.StartBehandlingSteg

object Dokumentflyt: BehandlingType {
    override fun flyt(): BehandlingFlyt = BehandlingFlytBuilder()
        .medSteg(steg = StartBehandlingSteg)
        .medSteg(steg = DigitaliserDokumentSteg)
        .medSteg(steg = OverleverTilFagsystemSteg)
        .medSteg(steg = AvsluttBehandlingSteg)
        .build()
}