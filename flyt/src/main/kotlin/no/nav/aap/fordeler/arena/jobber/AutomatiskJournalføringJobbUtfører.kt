package no.nav.aap.fordeler.arena.jobber

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProviderJobbSpesifikasjon
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.Fagsystem
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.postmottak.gateway.JournalføringsGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import org.slf4j.LoggerFactory

class AutomatiskJournalføringJobbUtfører(
    private val joarkClient: JournalføringsGateway,
    private val gosysOppgaveGateway: GosysOppgaveGateway,
    private val flytJobbRepository: FlytJobbRepository,
    private val innkommendeJournalpostRepository: InnkommendeJournalpostRepository,
    journalpostService: JournalpostService,
    val prometheus: MeterRegistry = SimpleMeterRegistry()
) : ArenaJobbutførerBase(journalpostService) {

    companion object : ProviderJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider): JobbUtfører {
            return AutomatiskJournalføringJobbUtfører(
                GatewayProvider.provide(JournalføringsGateway::class),
                GatewayProvider.provide(GosysOppgaveGateway::class),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                JournalpostService.konstruer(repositoryProvider, GatewayProvider),
                PrometheusProvider.prometheus
            )
        }

        override val type = "arena.automatiskjournalføring"

        override val navn = "Automatisk journalfører"

        override val beskrivelse = "Journalfører journalposter som kan behandles automatisk"

    }

    private var log = LoggerFactory.getLogger(this::class.java)

    override fun utførArena(input: JobbInput, journalpost: Journalpost) {
        val kontekst = input.getAutomatiskJournalføringKontekst()

        if (input.antallRetriesForsøkt() >= retries) {
            val enhet = innkommendeJournalpostRepository.hent(journalpost.journalpostId).enhet

            flytJobbRepository.leggTil(
                JobbInput(ManuellJournalføringJobbUtfører)
                    .medArenaVideresenderKontekst(
                        ArenaVideresenderKontekst.fra(journalpost, enhet, kontekst.innkommendeJournalpostId)
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
