package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.SattPåVentLøsning
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingOppgaveUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.motor.OppgaveInput
import no.nav.aap.motor.OppgaveRepository
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Status

class BehandlingHendelseHåndterer(connection: DBConnection) {

    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    private val avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection)
    private val avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(connection)
    private val oppgaveRepository = OppgaveRepository(connection)
    private val kontroller = FlytOrkestrator(connection)

    fun håndtere(key: BehandlingId, hendelse: BehandlingHendelse) {

        val behandling = behandlingRepository.hent(key)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        avklaringsbehovene.validateTilstand(behandling = behandling)

        when (hendelse) {
            is BehandlingSattPåVent -> {
                kontroller.settBehandlingPåVent(behandling.flytKontekst())
            }

            else -> {
                val kontekst = behandling.flytKontekst()
                if (behandling.status() == Status.PÅ_VENT) {
                    val avklaringsbehovKontroller = avklaringsbehovOrkestrator
                    avklaringsbehovKontroller.løsAvklaringsbehov(
                        kontekst = kontekst,
                        avklaringsbehovene = avklaringsbehovene,
                        avklaringsbehov = SattPåVentLøsning()
                    )
                }
                oppgaveRepository.leggTil(
                    OppgaveInput(oppgave = ProsesserBehandlingOppgaveUtfører).forBehandling(
                        kontekst.sakId,
                        kontekst.behandlingId
                    )
                )
            }
        }

    }
}