package no.nav.aap.postmottak.prosessering

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.fordeler.Enhetsutreder
import no.nav.aap.fordeler.FordelerRegelService
import no.nav.aap.fordeler.InnkommendeJournalpost
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.InnkommendeJournalpostStatus
import no.nav.aap.fordeler.NavEnhet
import no.nav.aap.fordeler.Regelresultat
import no.nav.aap.fordeler.regler.RegelInput
import no.nav.aap.fordeler.ÅrsakTilStatus
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
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
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.gateway.hoveddokument
import no.nav.aap.postmottak.gateway.originalFiltype
import no.nav.aap.postmottak.journalpostCounter
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.slf4j.LoggerFactory

class FordelingRegelJobbUtfører(
    private val flytJobbRepository: FlytJobbRepository,
    private val journalpostService: JournalpostService,
    private val regelService: FordelerRegelService,
    private val innkommendeJournalpostRepository: InnkommendeJournalpostRepository,
    private val gosysOppgaveGateway: GosysOppgaveGateway,
    private val enhetsutreder: Enhetsutreder,
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
                Enhetsutreder.konstruer(),
                PrometheusProvider.prometheus
            )
        }

        override fun type() = "fordel.innkommende"

        override fun navn() = "Prosesser fordeling"

        override fun beskrivelse() = "Vurderer mottaker av innkommende journalpost"

    }

    override fun utfør(input: JobbInput) {
        val journalpostId = input.getJournalpostId()

        // TODO: Denne kan være problematisk hvis vi skal støtte at journalførte dokumenter skal kunne sendes inn til Kelvin
        if (innkommendeJournalpostRepository.eksisterer(journalpostId)) {
            log.info("Journalposten har allerede blitt evaluert - behandler ikke videre")
            return
        }

        val safJournalpost = journalpostService.hentSafJournalpost(journalpostId)

        val statusMedÅrsakOgRegelresultat: StatusMedÅrsakOgRegelresultat = when {
            safJournalpost.journalstatus == Journalstatus.JOURNALFOERT -> {
                log.info("Journalposten har status ${safJournalpost.journalstatus} - behandler ikke videre")
                StatusMedÅrsakOgRegelresultat(
                    InnkommendeJournalpostStatus.IGNORERT,
                    ÅrsakTilStatus.ALLEREDE_JOURNALFØRT
                )
            }

            safJournalpost.journalstatus == Journalstatus.UTGAAR -> {
                log.info("Journalposten har status ${safJournalpost.journalstatus} - behandler ikke videre")
                StatusMedÅrsakOgRegelresultat(
                    InnkommendeJournalpostStatus.IGNORERT,
                    ÅrsakTilStatus.UTGÅTT
                )
            }

            safJournalpost.bruker?.id == null -> {
                val årsak = ÅrsakTilStatus.MANGLER_IDENT
                log.info("Bruker på ${safJournalpost.journalpostId} var ${safJournalpost.bruker?.type ?: "tom"} - oppretter fordelingsoppgave hvis ikke eksisterer")
                opprettFordelingsOppgaveHvisIkkeEksisterer(safJournalpost, årsak)
                StatusMedÅrsakOgRegelresultat(
                    InnkommendeJournalpostStatus.GOSYS_FDR,
                    årsak
                )
            }

            safJournalpost.bruker.type == BrukerIdType.ORGNR -> {
                val årsak = ÅrsakTilStatus.ORGNR
                log.info("Bruker på ${safJournalpost.journalpostId} var organisasjon - oppretter fordelingsoppgave hvis ikke eksisterer")
                opprettFordelingsOppgaveHvisIkkeEksisterer(safJournalpost, årsak)
                StatusMedÅrsakOgRegelresultat(
                    InnkommendeJournalpostStatus.GOSYS_FDR,
                    årsak
                )
            }

            else -> {
                val journalpost = journalpostService.tilJournalpostMedDokumentTitler(safJournalpost)

                val res = regelService.evaluer(
                    RegelInput(
                        safJournalpost.journalpostId,
                        journalpost.person,
                        journalpost.hoveddokumentbrevkode
                    )
                )
                StatusMedÅrsakOgRegelresultat(
                    InnkommendeJournalpostStatus.EVALUERT,
                    regelresultat = res
                )
            }
        }

        val id = innkommendeJournalpostRepository.lagre(
            InnkommendeJournalpost(
                journalpostId = JournalpostId(safJournalpost.journalpostId),
                brevkode = safJournalpost.hoveddokument()?.brevkode,
                behandlingstema = safJournalpost.behandlingstema,
                status = statusMedÅrsakOgRegelresultat.status,
                årsakTilStatus = statusMedÅrsakOgRegelresultat.årsak,
                enhet = hentEnhet(safJournalpost),
                regelresultat = statusMedÅrsakOgRegelresultat.regelresultat
            )
        )
        prometheus.journalpostCounter(
            brevkode = safJournalpost.hoveddokument()?.brevkode,
            filtype = safJournalpost.originalFiltype()
        ).increment()

        if (statusMedÅrsakOgRegelresultat.status == InnkommendeJournalpostStatus.EVALUERT) {
            opprettVideresendJobb(id, journalpostId)
        }
    }

    private fun hentEnhet(safJournalpost: SafJournalpost): NavEnhet? {
        return if (safJournalpost.bruker?.id == null) {
            log.warn("Journalpost med id=${safJournalpost.journalpostId} mangler bruker – kan ikke utlede enhet")
            null
        } else if (safJournalpost.bruker.type == BrukerIdType.ORGNR) {
            log.warn("Journalpost med id=${safJournalpost.journalpostId} har bruker med idType ORGNR – kan ikke utlede enhet")
            null
        } else {
            val journalpost = journalpostService.tilJournalpostMedDokumentTitler(safJournalpost)
            enhetsutreder.finnJournalføringsenhet(journalpost)
        }
    }

    private fun opprettVideresendJobb(innkommendeJournalpostId: Long, journalpostId: JournalpostId) {
        flytJobbRepository.leggTil(
            JobbInput(FordelingVideresendJobbUtfører)
                .forSak(journalpostId.referanse)
                .medJournalpostId(journalpostId)
                .medInnkommendeJournalpostId(innkommendeJournalpostId)
                .medCallId()
        )
    }

    private fun opprettFordelingsOppgaveHvisIkkeEksisterer(journalpost: SafJournalpost, årsak: ÅrsakTilStatus) {
        val tittel = journalpost.hoveddokument()?.tittel
            ?: throw IllegalStateException("Fant ingen dokumenter i journalposten")

        gosysOppgaveGateway.opprettFordelingsOppgaveHvisIkkeEksisterer(
            journalpostId = JournalpostId(journalpost.journalpostId),
            personIdent = null,
            orgnr = if (årsak == ÅrsakTilStatus.ORGNR) journalpost.bruker?.id else null,
            beskrivelse = tittel
        )
    }

    data class StatusMedÅrsakOgRegelresultat(
        val status: InnkommendeJournalpostStatus,
        val årsak: ÅrsakTilStatus? = null,
        val regelresultat: Regelresultat? = null
    )
}

