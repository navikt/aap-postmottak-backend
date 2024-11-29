package no.nav.aap.postmottak.fordeler.arena.jobber

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.klient.arena.ArenaKlient
import no.nav.aap.postmottak.klient.arena.ArenaOpprettOppgaveForespørsel
import org.slf4j.LoggerFactory

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

        override fun type() = "arena.søknad"

        override fun navn() = "Søknad til Arean Håndterer"

        override fun beskrivelse() = "Oppretter sak i Arena for ny søknad"

    }

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun utfør(input: JobbInput) {
        val kontekst = input.getArenaVideresenderKontekst()

        if (!arenaKlient.harAktivSak(kontekst.ident)) {
            log.info("Oppretter oppgave i Arena for søknad med journalpostid \"${kontekst.journalpostId}\"")
            val request = ArenaOpprettOppgaveForespørsel(
                fnr = kontekst.ident.identifikator,
                enhet = kontekst.navEnhet,
                tittel = kontekst.hoveddokumenttittel,
                titler = kontekst.vedleggstitler
            )
            log.info(request.toString()) // TODO  SLETT MEG ETTER DEBUGGING
            val sakId = arenaKlient.opprettArenaOppgave(request).arenaSakId
            opprettAutomatiskJournalføringsjobb(kontekst, sakId)
        } else {
            log.info("Det finnes alt en sak i Arena for ${kontekst.ident}, sender journalpost til manuell journalføring")
            opprettManuellJournalføringsoppgavejobb(kontekst)
        }

    }

    private fun opprettAutomatiskJournalføringsjobb(kontekst: ArenaVideresenderKontekst, arenaSakId :String) {
        flytJobbRepository.leggTil(
            JobbInput(AutomatiskJournalføringJobbUtfører)
                .medAutomatiskJournalføringKontekst(AutomatiskJournalføringKontekst(
                    journalpostId = kontekst.journalpostId,
                    ident = kontekst.ident,
                    saksnummer = arenaSakId,
                ))
                .medCallId()
        )
    }

    private fun opprettManuellJournalføringsoppgavejobb(kontekst: ArenaVideresenderKontekst) {
        flytJobbRepository.leggTil(
            JobbInput(ManuellJournalføringJobbUtfører)
                .medArenaVideresenderKontekst(kontekst)
                .medCallId()
        )
    }

}