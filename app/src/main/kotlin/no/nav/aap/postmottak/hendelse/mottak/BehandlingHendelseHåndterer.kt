package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.postmottak.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.postmottak.hendelse.avløp.BehandlingHendelseService
import no.nav.aap.postmottak.hendelse.mottak.BehandlingHendelse
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class BehandlingHendelseHåndterer(connection: DBConnection) {

    private val avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(
        connection,
        BehandlingHendelseService(FlytJobbRepository((connection)))
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