package no.nav.aap.behandlingsflyt.sak

import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.EndringType
import no.nav.aap.behandlingsflyt.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.behandling.Årsak
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Revurdering

class SakOgBehandlingService(connection: DBConnection) {

    private val sakRepository = sakRepository(connection)
    private val behandlingRepository = behandlingRepository(connection)
    private val grunnlagKopierer = GrunnlagKopierer(connection)

    fun finnEnRelevantBehandling(key: Saksnummer): Behandling {
        val sak = sakRepository.hent(key)

        val sisteBehandlingForSak = behandlingRepository.finnSisteBehandlingFor(sak.id)

        if (sisteBehandlingForSak == null) {
            return behandlingRepository.opprettBehandling(
                sak.id,
                listOf(Årsak(EndringType.MOTTATT_SØKNAD)),
                Førstegangsbehandling)

        } else {
            if (sisteBehandlingForSak.status().erAvsluttet()) {
                val nyBehandling = behandlingRepository.opprettBehandling(
                    sak.id,
                    listOf(Årsak(EndringType.MOTTATT_SØKNAD)),
                    Revurdering
                )
                grunnlagKopierer.overfør(sisteBehandlingForSak.id, nyBehandling.id)

                return nyBehandling

            } else {
                return sisteBehandlingForSak
            }
        }
    }
}