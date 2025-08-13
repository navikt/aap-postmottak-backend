package no.nav.aap.postmottak.forretningsflyt.behandlingstyper

import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostInformasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerInfoKrav
import no.nav.aap.postmottak.flyt.BehandlingFlyt
import no.nav.aap.postmottak.flyt.BehandlingFlytBuilder
import no.nav.aap.postmottak.flyt.BehandlingType
import no.nav.aap.postmottak.forretningsflyt.steg.StartBehandlingSteg
import no.nav.aap.postmottak.forretningsflyt.steg.dokumentflyt.AvsluttBehandlingSteg
import no.nav.aap.postmottak.forretningsflyt.steg.journalføring.AvklarSakSteg
import no.nav.aap.postmottak.forretningsflyt.steg.journalføring.AvklarTemaSteg
import no.nav.aap.postmottak.forretningsflyt.steg.journalføring.JournalføringSteg
import no.nav.aap.postmottak.forretningsflyt.steg.journalføring.SettFagsakSteg
import no.nav.aap.postmottak.forretningsflyt.steg.journalføring.VideresendSteg

object Journalføringsflyt : BehandlingType {
    override fun flyt(): BehandlingFlyt = BehandlingFlytBuilder()
        .medSteg(steg = StartBehandlingSteg)
        .medSteg(steg = AvklarTemaSteg, informasjonskrav = listOf(JournalpostInformasjonskrav, SaksnummerInfoKrav))
        .medSteg(steg = AvklarSakSteg)
        .medSteg(steg = SettFagsakSteg)
        .medSteg(steg = JournalføringSteg)
        .medSteg(steg = VideresendSteg)
        .medSteg(steg = AvsluttBehandlingSteg)
        .build()
}