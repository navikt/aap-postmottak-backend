package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.domene.Periode
import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.sak.SakRepository
import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.sak.SakService

object PeriodeTilVurderingTjeneste {

    fun utled(behandling: Behandling, vilkår: Vilkårtype): Set<Periode> {
        if (behandling.type == Førstegangsbehandling) {
            // ved førstegangsbehandling skal hele perioden alltid vurderes for alle vilkår?
            val sakService = SakService(null)
            val sak = sakService.hent(behandling.sakId)

            return setOf(sak.rettighetsperiode)
        }
        behandling.årsaker()
        TODO(" Sjekk vilkår mot årsaker til vurdering (ligger på behandling)")
    }
}