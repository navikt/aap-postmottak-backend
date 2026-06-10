package no.nav.aap.postmottak.forretningsflyt.behandlingstyper

import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostInformasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerInfoKrav
import no.nav.aap.postmottak.flyt.BehandlingFlyt
import no.nav.aap.postmottak.flyt.BehandlingFlytBuilder
import no.nav.aap.postmottak.flyt.BehandlingType
import no.nav.aap.postmottak.forretningsflyt.steg.dokumentflyt.AvsluttBehandlingSteg
import no.nav.aap.postmottak.forretningsflyt.steg.StartBehandlingSteg
import no.nav.aap.postmottak.forretningsflyt.steg.fordeling.AvklarFordelingSteg
import no.nav.aap.postmottak.forretningsflyt.steg.fordeling.FordelingVideresendSteg

object Fordelingflyt: BehandlingType {
    override fun flyt(): BehandlingFlyt = BehandlingFlytBuilder()
        .medSteg(steg = StartBehandlingSteg)
        .medSteg(steg = AvklarFordelingSteg, informasjonskrav = listOf(JournalpostInformasjonskrav))
        .medSteg(steg = FordelingVideresendSteg)
        .medSteg(steg = AvsluttBehandlingSteg)
        .build()
}