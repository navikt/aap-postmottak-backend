package no.nav.aap.postmottak.forretningsflyt.behandlingstyper

import no.nav.aap.postmottak.flyt.BehandlingFlyt
import no.nav.aap.postmottak.flyt.BehandlingFlytBuilder
import no.nav.aap.postmottak.flyt.BehandlingType
import no.nav.aap.postmottak.forretningsflyt.steg.StartBehandlingSteg
import no.nav.aap.postmottak.forretningsflyt.steg.dokumentflyt.AvsluttBehandlingSteg
import no.nav.aap.postmottak.forretningsflyt.steg.under18.AvklarUnder18SamtykkeSteg
import no.nav.aap.postmottak.forretningsflyt.steg.under18.AvklarUnder18Steg
import no.nav.aap.postmottak.forretningsflyt.steg.under18.OpprettUnder18SakSteg

object Under18Flyt : BehandlingType {
    override fun flyt(): BehandlingFlyt = BehandlingFlytBuilder()
        .medSteg(steg = StartBehandlingSteg)
        .medSteg(steg = AvklarUnder18Steg)
        .medSteg(steg = AvklarUnder18SamtykkeSteg)
        .medSteg(steg = OpprettUnder18SakSteg)
        .medSteg(steg = AvsluttBehandlingSteg)
        .build()
}
