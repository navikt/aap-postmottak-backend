package no.nav.aap.postmottak.behandling.avklaringsbehov

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.hendelse.avløp.BehandlingHendelseService
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class AvklaringsbehovHendelseHåndterer(connection: DBConnection) {

    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    private val avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection)
    private val avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(
        connection,
        BehandlingHendelseService(FlytJobbRepository(connection))
    )

    fun håndtere(key: BehandlingId, hendelse: LøsAvklaringsbehovBehandlingHendelse) {
        val behandling = behandlingRepository.hentMedLås(key, null)

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

        avklaringsbehovene.validateTilstand(
            behandling = behandling,
            avklaringsbehov = hendelse.behov().definisjon()
        )

        avklaringsbehovOrkestrator.løsAvklaringsbehovOgFortsettProsessering(
            kontekst = behandling.flytKontekst(),
            avklaringsbehov = hendelse.behov(),
            ingenEndringIGruppe = hendelse.ingenEndringIGruppe,
            bruker = hendelse.bruker
        )
    }
}