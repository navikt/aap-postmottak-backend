package no.nav.aap.postmottak.server.prosessering

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.motor.Jobb
import no.nav.aap.postmottak.fordeler.arena.jobber.AutomatiskJournalføringJobbUtfører
import no.nav.aap.postmottak.fordeler.arena.jobber.ManuellJournalføringJobbUtfører
import no.nav.aap.postmottak.fordeler.arena.jobber.SendSøknadTilArenaJobbUtfører

object ProsesseringsJobber {

    fun alle(prometheus: MeterRegistry = SimpleMeterRegistry()): List<Jobb> {
        // Legger her alle oppgavene som skal utføres i systemet
        return listOf(
            ProsesserBehandlingJobbUtfører,
            StoppetHendelseJobbUtfører,
            GjenopptaBehandlingJobbUtfører,
            FordelingRegelJobb(prometheus),
            FordelingVideresendJobb(prometheus),
            SendSøknadTilArenaJobbUtfører,
            ManuellJournalføringJobbUtfører,
            AutomatiskJournalføringJobbUtfører
        )
    }
}