package no.nav.aap.postmottak.forretningsflyt.behandlingstyper

import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.flyt.BehandlingFlyt
import no.nav.aap.postmottak.flyt.BehandlingFlytBuilder
import no.nav.aap.postmottak.flyt.BehandlingType
import no.nav.aap.postmottak.forretningsflyt.informasjonskrav.saksnummer.SaksnummerInfoKrav
import no.nav.aap.postmottak.forretningsflyt.steg.AvklarTemaSteg
import no.nav.aap.postmottak.forretningsflyt.steg.DigitaliserDokumentSteg
import no.nav.aap.postmottak.forretningsflyt.steg.FinnSakSteg
import no.nav.aap.postmottak.forretningsflyt.steg.JournalføringSteg
import no.nav.aap.postmottak.forretningsflyt.steg.KategoriserDokumentSteg
import no.nav.aap.postmottak.forretningsflyt.steg.OverleverTilFagsystemSteg
import no.nav.aap.postmottak.forretningsflyt.steg.SettFagsakSteg
import no.nav.aap.postmottak.forretningsflyt.steg.StartBehandlingSteg

object Dokumentflyt: BehandlingType {
    override fun flyt(): BehandlingFlyt = BehandlingFlytBuilder()
        .medSteg(steg = StartBehandlingSteg)
        .medSteg(steg = AvklarTemaSteg, informasjonskrav = listOf(JournalpostService))
        .medSteg(steg = FinnSakSteg, informasjonskrav = listOf(SaksnummerInfoKrav))
        .sluttÅOppdatereFaktagrunnlag()
        .medSteg(steg = SettFagsakSteg)
        .medSteg(steg = JournalføringSteg)
        .medSteg(steg = KategoriserDokumentSteg)
        .medSteg(steg = DigitaliserDokumentSteg)
        .medSteg(steg = OverleverTilFagsystemSteg)
        .build()
}