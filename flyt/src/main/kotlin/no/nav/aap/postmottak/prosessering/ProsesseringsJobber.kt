package no.nav.aap.postmottak.prosessering

import no.nav.aap.fordeler.arena.jobber.AutomatiskJournalføringJobbUtfører
import no.nav.aap.fordeler.arena.jobber.ManuellJournalføringJobbUtfører
import no.nav.aap.fordeler.arena.jobber.OppprettOppgaveIArenaJobbUtfører
import no.nav.aap.fordeler.arena.jobber.SendSøknadTilArenaJobbUtfører
import no.nav.aap.fordeler.arena.jobber.SendTilArenaKjørelisteBehandling
import no.nav.aap.motor.Jobb

object ProsesseringsJobber {

    fun alle(): List<Jobb> {
        // Legger her alle oppgavene som skal utføres i systemet
        return listOf(
            ProsesserBehandlingJobbUtfører,
            StoppetHendelseJobbUtfører,
            GjenopptaBehandlingJobbUtfører,
            FordelingRegelJobbUtfører,
            OppryddingJobbUtfører,
            FordelingVideresendJobbUtfører,
            SendSøknadTilArenaJobbUtfører,
            ManuellJournalføringJobbUtfører,
            AutomatiskJournalføringJobbUtfører,
            SendTilArenaKjørelisteBehandling,
            OppprettOppgaveIArenaJobbUtfører
        )
    }
}