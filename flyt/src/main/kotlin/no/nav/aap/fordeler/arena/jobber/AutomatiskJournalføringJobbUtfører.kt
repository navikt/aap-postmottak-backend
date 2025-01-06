package no.nav.aap.fordeler.arena.jobber

import no.nav.aap.fordeler.Enhetsutreder
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.JournalføringsGateway
import org.slf4j.LoggerFactory

class AutomatiskJournalføringJobbUtfører(
    private val joarkClient: JournalføringsGateway,
    private val flytJobbRepository: FlytJobbRepository,
    private val journalpostService: JournalpostService,
    private val enhetsutreder: Enhetsutreder
) : JobbUtfører {

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return AutomatiskJournalføringJobbUtfører(
                GatewayProvider.provide(JournalføringsGateway::class),
                FlytJobbRepository(connection),
                JournalpostService.konstruer(connection),
                Enhetsutreder.konstruer()
            )
        }

        override fun type() = "arena.automatiskjournalføring"

        override fun navn() = "Automatisk journalfører"

        override fun beskrivelse() = "Journalfører journalposter som kan behandles automatisk"

    }

    private var log = LoggerFactory.getLogger(this::class.java)

    override fun utfør(input: JobbInput) {
        val kontekst = input.getAutomatiskJournalføringKontekst()

        if (input.antallRetriesForsøkt() >= retries()) {
            val journalpost = journalpostService.hentjournalpost(kontekst.journalpostId)
            val enhet = enhetsutreder.finnNavenhetForJournalpost(journalpost)
            flytJobbRepository.leggTil(
                JobbInput(ManuellJournalføringJobbUtfører)
                    .medArenaVideresenderKontekst(journalpost.opprettArenaVideresenderKontekst(enhet))
            )
            return
        }
        
        log.info("Automatisk journalfører journalpost ${kontekst.journalpostId} på sak ${kontekst.saksnummer} ")
        joarkClient.førJournalpostPåFagsak(kontekst.journalpostId, kontekst.ident, kontekst.saksnummer)
        joarkClient.ferdigstillJournalpostMaskinelt(kontekst.journalpostId)
    }

}
