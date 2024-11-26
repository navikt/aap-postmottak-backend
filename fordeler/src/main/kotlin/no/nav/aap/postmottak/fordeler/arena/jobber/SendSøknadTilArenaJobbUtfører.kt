package no.nav.aap.postmottak.fordeler.arena.jobber

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.klient.arena.ArenaKlient
import no.nav.aap.postmottak.klient.arena.ArenaOpprettOppgaveForespørsel

class SendSøknadTilArenaJobbUtfører(
    private val flytJobbRepository: FlytJobbRepository,
    private val arenaKlient: ArenaKlient
) : JobbUtfører {

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return SendSøknadTilArenaJobbUtfører(
                FlytJobbRepository(connection),
                ArenaKlient()
            )
        }

        override fun type() = "arena.oppgaveoppretter"

        override fun navn() = "Opprett oppgave i Arena"

        override fun beskrivelse() = "Oppretter oppgave i Arena for ny Søknad om AAP"

    }

    override fun utfør(input: JobbInput) {
        val kontekst = input.getArenaVideresenderKontekst()

        if (!arenaKlient.harAktivSak(kontekst.ident)) {
            arenaKlient.opprettArenaOppgave(ArenaOpprettOppgaveForespørsel(
                fnr = kontekst.ident.identifikator,
                enhet = kontekst.navEnhet,
                tittel = kontekst.hoveddokumenttittel,
                titler = kontekst.vedleggstitler
            ))
            opprettAutomatiskJournalføringsjobb(kontekst)
        } else {
            opprettManuellJournalføringsoppgavejobb(kontekst)
        }

    }

    private fun opprettAutomatiskJournalføringsjobb(kontekst: ArenaVideresenderKontekst) {
        flytJobbRepository.leggTil(
            JobbInput(AutomatiskJournalføringsJobbUtfører)
                .medArenaVideresenderKontekst(kontekst)
        )
    }

    private fun opprettManuellJournalføringsoppgavejobb(kontekst: ArenaVideresenderKontekst) {
        flytJobbRepository.leggTil(
            JobbInput(ManuellJournalføringsoppgaveJobbUtfører)
                .medArenaVideresenderKontekst(kontekst)
        )
    }

}