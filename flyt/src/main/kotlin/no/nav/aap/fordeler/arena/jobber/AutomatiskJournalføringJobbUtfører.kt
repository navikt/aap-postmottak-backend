package no.nav.aap.fordeler.arena.jobber

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.fordeler.Enhetsutreder
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.Fagsystem
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.postmottak.gateway.JournalføringsGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.JournalpostMedDokumentTitler
import org.slf4j.LoggerFactory

class AutomatiskJournalføringJobbUtfører(
    private val joarkClient: JournalføringsGateway,
    private val gosysOppgaveGateway: GosysOppgaveGateway,
    private val flytJobbRepository: FlytJobbRepository,
    journalpostService: JournalpostService,
    private val enhetsutreder: Enhetsutreder,
    val prometheus: MeterRegistry = SimpleMeterRegistry()
) : ArenaJobbutførerBase(journalpostService) {

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return AutomatiskJournalføringJobbUtfører(
                GatewayProvider.provide(JournalføringsGateway::class),
                GatewayProvider.provide(GosysOppgaveGateway::class),
                FlytJobbRepository(connection),
                JournalpostService.konstruer(connection),
                Enhetsutreder.konstruer(),
                PrometheusProvider.prometheus
            )
        }

        override fun type() = "arena.automatiskjournalføring"

        override fun navn() = "Automatisk journalfører"

        override fun beskrivelse() = "Journalfører journalposter som kan behandles automatisk"

    }

    private var log = LoggerFactory.getLogger(this::class.java)

    override fun utførArena(input: JobbInput, journalpost: JournalpostMedDokumentTitler) {
        val kontekst = input.getAutomatiskJournalføringKontekst()

        if (input.antallRetriesForsøkt() >= retries()) {
            val enhet = enhetsutreder.finnJournalføringsenhet(journalpost)
            flytJobbRepository.leggTil(
                JobbInput(ManuellJournalføringJobbUtfører)
                    .medArenaVideresenderKontekst(
                        journalpost.opprettArenaVideresenderKontekst(
                            enhet,
                            kontekst.innkommendeJournalpostId
                        )
                    )
            )
            return
        }

        log.info("Automatisk journalfører journalpost ${kontekst.journalpostId} på sak ${kontekst.saksnummer} ")
        joarkClient.førJournalpostPåFagsak(
            journalpost.journalpostId,
            kontekst.ident,
            kontekst.saksnummer,
            fagsystem = Fagsystem.AO01
        )
        joarkClient.ferdigstillJournalpostMaskinelt(kontekst.journalpostId)

        gosysOppgaveGateway.finnOppgaverForJournalpost(journalpost.journalpostId, tema = "AAP")
            .forEach { gosysOppgaveGateway.ferdigstillOppgave(it) }
    }

}
