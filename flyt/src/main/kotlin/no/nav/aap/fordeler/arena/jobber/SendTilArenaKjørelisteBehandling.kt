package no.nav.aap.fordeler.arena.jobber

import no.nav.aap.fordeler.arena.ArenaGateway
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.JournalpostMedDokumentTitler
import org.slf4j.LoggerFactory

class SendTilArenaKjørelisteBehandling(
    private val flytJobbRepository: FlytJobbRepository,
    private val arenaKlient: ArenaGateway,
    journalpostService: JournalpostService
) : ArenaJobbutførerBase(journalpostService) {

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return SendTilArenaKjørelisteBehandling(
                FlytJobbRepository(connection),
                GatewayProvider.provide(ArenaGateway::class),
                JournalpostService.konstruer(connection)
            )
        }

        override fun type() = "arena.kjøreliste"

        override fun navn() = "Kjøreliste til Arean Håndterer"

        override fun beskrivelse() = "Behandle kjoereliste og opprett oppgave"

        override fun retries() = 4

    }

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun utførArena(input: JobbInput, journalpost: JournalpostMedDokumentTitler) {
        val kontekst = input.getArenaVideresenderKontekst()

        if (input.antallRetriesForsøkt() >= 2) {
            log.info("Forsøk på sending av kjøreliste til Arena feilet ${input.antallRetriesForsøkt()+1}, oppretter manuell oppgave")
            opprettManuellJournalføringsoppgavejobb(kontekst)
            return
        }

        log.info("Oppretter oppgave i Arena for søknad med journalpostid \"${kontekst.journalpostId}\"")
        /**
         * Dette fungerer ikke i dag - mangler xml
         */
        val sakId = arenaKlient.behandleKjoerelisteOgOpprettOppgave(kontekst.journalpostId)
        opprettAutomatiskJournalføringsjobb(kontekst, sakId)
    }

    private fun opprettAutomatiskJournalføringsjobb(kontekst: ArenaVideresenderKontekst, arenaSakId :String) {
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