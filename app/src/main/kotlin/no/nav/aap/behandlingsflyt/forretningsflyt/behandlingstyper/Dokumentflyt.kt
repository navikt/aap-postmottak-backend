package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.forretningsflyt.informasjonskrav.saksnummer.SaksnummerInfoKrav
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.AvklarTemaSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.DigitaliserDokumentSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FinnSakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.JournalføringSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.KategoriserDokumentSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.OverleverTilFagsystemSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.StartBehandlingSteg

object Dokumentflyt: BehandlingType {
    override fun flyt(): BehandlingFlyt = BehandlingFlytBuilder()
        .medSteg(steg = StartBehandlingSteg)
        .medSteg(steg = AvklarTemaSteg, informasjonskrav = listOf(JournalpostService))
        .medSteg(steg = FinnSakSteg, informasjonskrav = listOf(SaksnummerInfoKrav))
        .medSteg(steg = JournalføringSteg)
        .sluttÅOppdatereFaktagrunnlag()
        .medSteg(steg = KategoriserDokumentSteg)
        .medSteg(steg = DigitaliserDokumentSteg)
        .medSteg(steg = OverleverTilFagsystemSteg)
        .build()
}