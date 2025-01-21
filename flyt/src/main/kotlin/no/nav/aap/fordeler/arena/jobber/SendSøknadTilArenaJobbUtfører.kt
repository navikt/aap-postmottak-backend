package no.nav.aap.fordeler.arena.jobber

import no.nav.aap.fordeler.arena.ArenaGateway
import no.nav.aap.fordeler.arena.ArenaOppgaveType
import no.nav.aap.fordeler.arena.ArenaOpprettOppgaveForespørsel
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import org.slf4j.LoggerFactory

class SendSøknadTilArenaJobbUtfører(
    private val flytJobbRepository: FlytJobbRepository,
    private val arenaKlient: ArenaGateway
) : JobbUtfører {

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return SendSøknadTilArenaJobbUtfører(
                FlytJobbRepository(connection),
                GatewayProvider.provide(ArenaGateway::class)
            )
        }

        override fun type() = "arena.søknad"

        override fun navn() = "Søknad til Arean Håndterer"

        override fun beskrivelse() = "Oppretter sak i Arena for ny søknad"

        override fun retries() = 4

    }

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun utfør(input: JobbInput) {
        val kontekst = input.getArenaVideresenderKontekst()

        if (input.antallRetriesForsøkt() >= 2) {
            log.info("Forsøk på opprettelse av oppgave i Arena feilet ${input.antallRetriesForsøkt() + 1}, oppretter manuell oppgave")
            opprettManuellJournalføringsoppgavejobb((kontekst))
        } else if (kontekst.navEnhet != null && !arenaKlient.harAktivSak(kontekst.ident)) {
            log.info("Oppretter oppgave i Arena for søknad med journalpostid \"${kontekst.journalpostId}\"")
            val request = ArenaOpprettOppgaveForespørsel(
                fnr = kontekst.ident.identifikator,
                enhet = kontekst.navEnhet,
                tittel = kontekst.hoveddokumenttittel,
                titler = kontekst.vedleggstitler,
                oppgaveType = ArenaOppgaveType.STARTVEDTAK
            )
            val respons = arenaKlient.opprettArenaOppgave(request)
            log.info("Opprettet oppgave med id ${respons.oppgaveId} på sak ${respons.arenaSakId}")
            opprettAutomatiskJournalføringsjobb(kontekst, respons.arenaSakId)
        } else {
            log.info("Det finnes alt en sak i Arena for ${kontekst.ident}, sender journalpost til manuell journalføring")
            opprettManuellJournalføringsoppgavejobb(kontekst)
        }
    }

    private fun opprettAutomatiskJournalføringsjobb(kontekst: ArenaVideresenderKontekst, arenaSakId: String) {
        flytJobbRepository.leggTil(
            JobbInput(AutomatiskJournalføringJobbUtfører)
                .medAutomatiskJournalføringKontekst(
                    AutomatiskJournalføringKontekst(
                        journalpostId = kontekst.journalpostId,
                        ident = kontekst.ident,
                        saksnummer = arenaSakId,
                    )
                )
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