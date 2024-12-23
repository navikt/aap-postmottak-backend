package no.nav.aap.postmottak.hendelse.mottak

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

class BehandlingHendelseHåndterer(connection: DBConnection) {

    private val avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(
        connection,
        BehandlingHendelseServiceImpl(
            FlytJobbRepository(connection),
            RepositoryProvider(connection).provide(JournalpostRepository::class)
        )
    )

    fun håndtere(key: BehandlingId, hendelse: BehandlingHendelse) {
        when (hendelse) {
            is BehandlingSattPåVent -> {
                avklaringsbehovOrkestrator.settBehandlingPåVent(key, hendelse)
            }

            else -> {
                avklaringsbehovOrkestrator.taAvVentHvisPåVentOgFortsettProsessering(key)
            }
        }
    }
}