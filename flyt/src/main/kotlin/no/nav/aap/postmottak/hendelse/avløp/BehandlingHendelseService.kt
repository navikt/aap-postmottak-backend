package no.nav.aap.postmottak.hendelse.avløp

import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling

interface BehandlingHendelseService {
    fun stoppet(behandling: Behandling, avklaringsbehovene: Avklaringsbehovene)
}