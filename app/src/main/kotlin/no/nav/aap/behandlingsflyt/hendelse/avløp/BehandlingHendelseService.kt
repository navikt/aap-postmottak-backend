package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(BehandlingHendelseService::class.java)

class BehandlingHendelseService {

    fun stoppet(behandling: Behandling, avklaringsbehovene: Avklaringsbehovene) {
        // TODO: Slippe ut event om at behandlingen har stoppet opp
    }
}