package no.nav.aap.postmottak.server.prosessering

import no.nav.aap.motor.Jobb
import no.nav.aap.postmottak.fordeler.arena.jobber.AutomatiskJournalføringJobbUtfører
import no.nav.aap.postmottak.fordeler.arena.jobber.ManuellJournalføringJobbUtfører
import no.nav.aap.postmottak.fordeler.arena.jobber.SendSøknadTilArenaJobbUtfører

object ProsesseringsJobber {

    fun alle(): List<Jobb> {
        // Legger her alle oppgavene som skal utføres i systemet
        return listOf(
            ProsesserBehandlingJobbUtfører,
            StoppetHendelseJobbUtfører,
            GjenopptaBehandlingJobbUtfører,
            FordelingRegelJobbUtfører,
            FordelingVideresendJobbUtfører,
            SendSøknadTilArenaJobbUtfører,
            ManuellJournalføringJobbUtfører,
            AutomatiskJournalføringJobbUtfører
        )
    }
}