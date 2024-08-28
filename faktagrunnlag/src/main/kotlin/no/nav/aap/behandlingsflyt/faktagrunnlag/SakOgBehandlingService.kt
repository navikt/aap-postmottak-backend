package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.BehandlingTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.BeriketBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling

class SakOgBehandlingService(connection: DBConnection) {

    private val behandlingRepository = BehandlingRepositoryImpl(connection)

    fun finnEllerOpprettBehandling(key: Saksnummer, årsaker: List<Årsak>): BeriketBehandling {


        return BeriketBehandling(
            behandling = behandlingRepository.opprettBehandling(
                TypeBehandling.DokumentHåndtering
            ), tilstand = BehandlingTilstand.NY, sisteAvsluttedeBehandling = null
        )


    }

}