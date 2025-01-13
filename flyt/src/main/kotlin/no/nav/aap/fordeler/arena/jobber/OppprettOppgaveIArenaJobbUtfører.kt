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

class OppprettOppgaveIArenaJobbUtfører(
    private val flytJobbRepository: FlytJobbRepository,
    private val arenaKlient: ArenaGateway
) : JobbUtfører {

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return OppprettOppgaveIArenaJobbUtfører(
                FlytJobbRepository(connection),
                GatewayProvider.provide(ArenaGateway::class)
            )
        }

        override fun type() = "arena.oppgaveoppretter"

        override fun navn() = "Arenaoppgaveoppretter"

        override fun beskrivelse() = "Oppretter oppgave i Arena for innkommende journalpost"

        override fun retries() = 4

    }

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun utfør(input: JobbInput) {
        val kontekst = input.getArenaVideresenderKontekst()

        if (input.antallRetriesForsøkt() >= 2) {
            log.info("Forsøk på opprettelse av oppgave i Arena feilet ${input.antallRetriesForsøkt() + 1}, oppretter manuell oppgave")
            opprettManuellJournalføringsoppgavejobb(kontekst)
            return
        }

        val saksId = arenaKlient.nyesteAktiveSak(kontekst.ident) ?: error("Fant ikke arenasaksnummer")

        log.info("Oppretter oppgave i Arena for journalpost: \"${kontekst.journalpostId}\"")
        val request = ArenaOpprettOppgaveForespørsel(
            fnr = kontekst.ident.identifikator,
            enhet = kontekst.navEnhet,
            tittel = kontekst.hoveddokumenttittel,
            titler = kontekst.vedleggstitler,
            oppgaveType = ArenaOppgaveType.BEHENVPERSON
        )
        arenaKlient.opprettArenaOppgave(request)
        opprettAutomatiskJournalføringsjobb(kontekst, saksId)

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