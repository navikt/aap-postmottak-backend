package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.domene.behandling.Vilkårstype
import no.nav.aap.behandlingsflyt.domene.sak.Sakslager
import no.nav.aap.behandlingsflyt.flyt.StegType

class StartBehandlingSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        // TODO: Init vilkår
        val behandling = BehandlingTjeneste.hent(input.kontekst.behandlingId)

        if (behandling.type == Førstegangsbehandling) {
            val vilkårsresultat = behandling.vilkårsresultat()
            val rettighetsperiode = Sakslager.hent(behandling.sakId).rettighetsperiode
            Vilkårstype.entries.forEach { vilkårstype -> vilkårsresultat.leggTilHvisIkkeEksisterer(vilkårstype).leggTilIkkeVurdertPeriode(rettighetsperiode) }
        }

        return StegResultat()
    }

    override fun type(): StegType {
        return StegType.START_BEHANDLING
    }
}
