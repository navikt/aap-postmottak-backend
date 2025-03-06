package no.nav.aap.postmottak.prosessering

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.fordeler.FordelerRegelService
import no.nav.aap.fordeler.InnkommendeJournalpost
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.InnkommendeJournalpostStatus
import no.nav.aap.fordeler.regler.RegelInput
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.BrukerIdType
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostCounter
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.slf4j.LoggerFactory

class FordelingRegelJobbUtfører(
    private val flytJobbRepository: FlytJobbRepository,
    private val journalpostService: JournalpostService,
    private val regelService: FordelerRegelService,
    private val innkommendeJournalpostRepository: InnkommendeJournalpostRepository,
    private val gosysOppgaveGateway: GosysOppgaveGateway,
    private val prometheus: MeterRegistry = SimpleMeterRegistry(),
) : JobbUtfører {
    private val log = LoggerFactory.getLogger(FordelingRegelJobbUtfører::class.java)

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            val repositoryProvider = RepositoryProvider(connection)
            return FordelingRegelJobbUtfører(
                FlytJobbRepository(connection),
                JournalpostService.konstruer(connection),
                FordelerRegelService(connection),
                repositoryProvider.provide(InnkommendeJournalpostRepository::class),
                GatewayProvider.provide(GosysOppgaveGateway::class),
                PrometheusProvider.prometheus
            )
        }

        override fun type() = "fordel.innkommende"

        override fun navn() = "Prosesser fordeling"

        override fun beskrivelse() = "Vurderer mottaker av innkommende journalpost"

    }

    override fun utfør(input: JobbInput) {
        val journalpostId = input.getJournalpostId()

        if (innkommendeJournalpostRepository.eksisterer(journalpostId)) {
            log.info("Journalposten har allerede blitt evaluert - behandler ikke videre")
            return
        }
        
        // TODO: Håndter organisasjonsnummer bedre
        val journalpost = try {
            journalpostService.hentJournalpostMedDokumentTitler(journalpostId)
        } catch (e: Exception) {
            // beklager
            if (e.message?.contains("Ugyldig ident ved GraphQL oppslag") == true
                || e.message?.contains("journalpost må ha ident") == true) {
                val safJournalpost = journalpostService.hentSafJournalpost(journalpostId)
                if (safJournalpost.journalstatus == Journalstatus.JOURNALFOERT) {
                    log.info("Journalposten er allerede journalført - behandler ikke videre")
                    return
                } else if (safJournalpost.journalstatus == Journalstatus.UTGAAR) {
                    log.info("Journalposten er utgått - behandler ikke videre")
                    return
                }

                if (safJournalpost.bruker?.type == BrukerIdType.ORGNR || safJournalpost.bruker?.id == null) {
                        gosysOppgaveGateway.opprettFordelingsOppgaveHvisIkkeEksisterer(
                            journalpostId = journalpostId,
                            personIdent = safJournalpost.bruker?.id?.let(::Ident),
                            beskrivelse = safJournalpost.dokumenter!!.minBy { it?.dokumentInfoId!! }?.tittel
                                ?: throw IllegalStateException("Fant ingen dokumenter i journalposten")
                        )

                        val årsak = if (safJournalpost.bruker?.type == BrukerIdType.ORGNR) "orgnummer" else "ingen bruker.id"
                        log.info("Fant $årsak på ${journalpostId} - opprettet fordelingsoppgave")
                    return
                } else throw e
            }
            throw e
        }
        log.info("Journalstatus: ${journalpost.status}")
        if (journalpost.status == Journalstatus.JOURNALFOERT) {
            log.info("Journalposten er allerede journalført - behandler ikke videre")
            return
        } else if (journalpost.status == Journalstatus.UTGAAR) {
            log.info("Journalposten er utgått - behandler ikke videre")
            return
        }
        log.info("Evaluerer regler...")

        val res = regelService.evaluer(
            RegelInput(
                journalpostId.referanse,
                journalpost.person,
                journalpost.hoveddokumentbrevkode
            )
        )

        val innkommendeJournalpost = InnkommendeJournalpost(
            journalpostId = journalpostId,
            brevkode = journalpost.hoveddokumentbrevkode,
            behandlingstema = journalpost.behandlingstema,
            status = InnkommendeJournalpostStatus.EVALUERT,
            regelresultat = res
        )

        innkommendeJournalpostRepository.lagre(innkommendeJournalpost)
        opprettVideresendJobb(journalpostId)

        prometheus.journalpostCounter(journalpost).increment()
    }

    private fun opprettVideresendJobb(journalpostId: JournalpostId) {
        flytJobbRepository.leggTil(
            JobbInput(FordelingVideresendJobbUtfører)
                .forSak(journalpostId.referanse)
                .medJournalpostId(journalpostId)
                .medCallId()
        )
    }
}