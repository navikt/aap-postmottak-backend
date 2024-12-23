package no.nav.aap.postmottak.forretningsflyt.behandlingstyper

import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerInfoKrav
import no.nav.aap.postmottak.flyt.BehandlingFlyt
import no.nav.aap.postmottak.flyt.BehandlingFlytBuilder
import no.nav.aap.postmottak.flyt.BehandlingType
import no.nav.aap.postmottak.forretningsflyt.steg.AvklarSakSteg
import no.nav.aap.postmottak.forretningsflyt.steg.AvklarTemaSteg
import no.nav.aap.postmottak.forretningsflyt.steg.JournalføringSteg
import no.nav.aap.postmottak.forretningsflyt.steg.SettFagsakSteg
import no.nav.aap.postmottak.forretningsflyt.steg.StartBehandlingSteg
import no.nav.aap.postmottak.forretningsflyt.steg.VideresendSteg

object Journalføringsflyt: BehandlingType {
    override fun flyt(): BehandlingFlyt = BehandlingFlytBuilder()
        .medSteg(steg = StartBehandlingSteg)
        .medSteg(steg = AvklarTemaSteg, informasjonskrav = listOf(JournalpostService))
        .medSteg(steg = AvklarSakSteg, informasjonskrav = listOf(SaksnummerInfoKrav))
        .medSteg(steg = SettFagsakSteg)
        .medSteg(steg = JournalføringSteg)
        .medSteg(steg = VideresendSteg)
        .build()
}