package no.nav.aap.postmottak.mottak

import no.nav.aap.postmottak.prosessering.getJournalpostId
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.fordeler.Enhetsutreder
import no.nav.aap.fordeler.InnkommendeJournalpost
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.InnkommendeJournalpostStatus
import no.nav.aap.fordeler.NavEnhet
import no.nav.aap.fordeler.ÅrsakTilStatus
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.BrukerIdType
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.gateway.hoveddokument
import no.nav.aap.postmottak.gateway.originalFiltype
import no.nav.aap.postmottak.journalpostCounter
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import org.slf4j.LoggerFactory

class VurderRelevantDokumentForAAPJobbUtfører(
    private val flytJobbRepository: FlytJobbRepository,
    val behandlingRepository: BehandlingRepository,
    private val journalpostService: JournalpostService,
    private val innkommendeJournalpostRepository: InnkommendeJournalpostRepository,
    private val gosysOppgaveGateway: GosysOppgaveGateway,
    private val enhetsutreder: Enhetsutreder,
    private val prometheus: MeterRegistry = SimpleMeterRegistry(),
) : JobbUtfører {
    private val log = LoggerFactory.getLogger(VurderRelevantDokumentForAAPJobbUtfører::class.java)

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return VurderRelevantDokumentForAAPJobbUtfører(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                JournalpostService.konstruer(repositoryProvider, gatewayProvider),
                repositoryProvider.provide(),
                gatewayProvider.provide(),
                Enhetsutreder.konstruer(gatewayProvider),
                PrometheusProvider.prometheus
            )
        }

        override val type = "vurderdokument.innkommende"
        override val navn = "Vurder relevant dokument"
        override val beskrivelse = "Vurderer om mottatt dokument er relevant for videre behandling eller oppretter fordelingoppgave"

    }

    override fun utfør(input: JobbInput) {
        val journalpostId = input.getJournalpostId()

        // TODO: Denne kan være problematisk hvis vi skal støtte at journalførte dokumenter skal kunne sendes inn til Kelvin
        if (innkommendeJournalpostRepository.eksisterer(journalpostId)) {
            log.info("Journalposten med ID (${journalpostId}) har allerede blitt evaluert - behandler ikke videre")
            return
        }

        val safJournalpost = journalpostService.hentSafJournalpost(journalpostId)

        val statusMedÅrsakOgRegelresultat: StatusMedÅrsak = when {
            safJournalpost.journalstatus == Journalstatus.JOURNALFOERT -> {
                log.info("Journalposten har status ${safJournalpost.journalstatus} - behandler ikke videre")
                StatusMedÅrsak(
                    InnkommendeJournalpostStatus.IGNORERT,
                    ÅrsakTilStatus.ALLEREDE_JOURNALFØRT
                )
            }

            safJournalpost.journalstatus == Journalstatus.UTGAAR -> {
                log.info("Journalposten har status ${safJournalpost.journalstatus} - behandler ikke videre")
                StatusMedÅrsak(
                    InnkommendeJournalpostStatus.IGNORERT,
                    ÅrsakTilStatus.UTGÅTT
                )
            }

            safJournalpost.bruker?.id == null -> {
                val årsak = ÅrsakTilStatus.MANGLER_IDENT
                log.info("Bruker på ${safJournalpost.journalpostId} var ${safJournalpost.bruker?.type ?: "tom"} - oppretter fordelingsoppgave hvis ikke eksisterer")
                opprettFordelingsOppgaveHvisIkkeEksisterer(safJournalpost, årsak)
                StatusMedÅrsak(
                    InnkommendeJournalpostStatus.GOSYS_FDR,
                    årsak
                )
            }

            safJournalpost.bruker.type == BrukerIdType.ORGNR -> {
                val årsak = ÅrsakTilStatus.ORGNR
                log.info("Bruker på ${safJournalpost.journalpostId} var organisasjon - oppretter fordelingsoppgave hvis ikke eksisterer")
                opprettFordelingsOppgaveHvisIkkeEksisterer(safJournalpost, årsak)
                StatusMedÅrsak(
                    InnkommendeJournalpostStatus.GOSYS_FDR,
                    årsak
                )
            }

            else -> {
                val journalpost = journalpostService.tilJournalpostMedDokumentTitler(safJournalpost)
                log.info("Evaluerer journalpost med ID ${journalpost.journalpostId}. Brevkode: ${journalpost.hoveddokumentbrevkode}. Dokumentet behandles videre i normal flyt.")
                StatusMedÅrsak(
                    InnkommendeJournalpostStatus.EVALUERT,
                )
            }
        }

        innkommendeJournalpostRepository.lagre(
            InnkommendeJournalpost(
                journalpostId = JournalpostId(safJournalpost.journalpostId),
                brevkode = safJournalpost.hoveddokument()?.brevkode,
                behandlingstema = safJournalpost.behandlingstema,
                status = statusMedÅrsakOgRegelresultat.status,
                årsakTilStatus = statusMedÅrsakOgRegelresultat.årsak,
                enhet = hentEnhet(safJournalpost),
                brukerId = safJournalpost.bruker?.takeIf { it.type != BrukerIdType.ORGNR }?.id,
            )
        )
        prometheus.journalpostCounter(
            brevkode = safJournalpost.hoveddokument()?.brevkode,
            filtype = safJournalpost.originalFiltype()
        ).increment()

        if (statusMedÅrsakOgRegelresultat.status == InnkommendeJournalpostStatus.EVALUERT) {
            startFordelingsflytForVidereBehandlingAvDokument(journalpostId)
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

    private fun startFordelingsflytForVidereBehandlingAvDokument(journalpostId: JournalpostId) {
        val behandling = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Fordeling)
        flytJobbRepository.leggTil(
            JobbInput(ProsesserBehandlingJobbUtfører)
                .forBehandling(sakID = journalpostId.referanse, behandlingId = behandling.id)
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

    data class StatusMedÅrsak(
        val status: InnkommendeJournalpostStatus,
        val årsak: ÅrsakTilStatus? = null
    )
}

