package no.nav.aap.behandlingsflyt.periodisering

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling

class PerioderTilVurderingService(connection: DBConnection) {
    private val sakService: SakService = SakService(connection)

    fun utled(kontekst: FlytKontekst, stegType: StegType): Set<Periode> {
        val sak = sakService.hent(kontekst.sakId)
        if (kontekst.behandlingType == TypeBehandling.Førstegangsbehandling) {
            // ved førstegangsbehandling skal hele perioden alltid vurderes for alle vilkår?

            return setOf(sak.rettighetsperiode)
        }

        // TODO(" Sjekk vilkår/steg mot årsaker til vurdering (ligger på behandling)")
        // Skal regne ut gitt hva som har skjedd på en behandling og hvilke perioder som er relevant at vi vurderer

        return setOf(sak.rettighetsperiode)
    }
}