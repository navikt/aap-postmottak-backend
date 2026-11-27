package no.nav.aap.postmottak.forretningsflyt.behandlingstyper

import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostInformasjonskrav
import no.nav.aap.postmottak.flyt.BehandlingFlyt
import no.nav.aap.postmottak.flyt.BehandlingFlytBuilder
import no.nav.aap.postmottak.flyt.BehandlingType
import no.nav.aap.postmottak.forretningsflyt.steg.StartBehandlingSteg
import no.nav.aap.postmottak.forretningsflyt.steg.dokumentflyt.AvsluttBehandlingSteg
import no.nav.aap.postmottak.forretningsflyt.steg.under18.AvklarUnder18SamtykkeSteg
import no.nav.aap.postmottak.forretningsflyt.steg.under18.AvklarUnder18Steg
import no.nav.aap.postmottak.forretningsflyt.steg.under18.OpprettUnder18DokumentbehandlingerSteg
import no.nav.aap.postmottak.forretningsflyt.steg.under18.OpprettUnder18SakSteg
import no.nav.aap.postmottak.under18.Under18FødselsdatoInformasjonskrav
import no.nav.aap.postmottak.under18.Under18JournalposterInformasjonskrav

object Under18Flyt : BehandlingType {
    override fun flyt(): BehandlingFlyt = BehandlingFlytBuilder()
        .medSteg(steg = StartBehandlingSteg)
        .medSteg(
            steg = AvklarUnder18Steg,
            informasjonskrav = listOf(
                JournalpostInformasjonskrav,
                Under18FødselsdatoInformasjonskrav,
                Under18JournalposterInformasjonskrav,
            )
        )
        .medSteg(steg = AvklarUnder18SamtykkeSteg)
        .medSteg(steg = OpprettUnder18SakSteg)
        .medSteg(steg = OpprettUnder18DokumentbehandlingerSteg)
        .medSteg(steg = AvsluttBehandlingSteg)
        .build()
}
