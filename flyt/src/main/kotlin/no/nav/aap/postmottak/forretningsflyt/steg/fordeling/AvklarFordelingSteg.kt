package no.nav.aap.postmottak.forretningsflyt.steg.fordeling

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.fordeler.Enhetsutreder
import no.nav.aap.fordeler.FordelerRegelService
import no.nav.aap.fordeler.InnkommendeJournalpost
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.InnkommendeJournalpostStatus
import no.nav.aap.fordeler.NavEnhet
import no.nav.aap.fordeler.Regelresultat
import no.nav.aap.fordeler.arena.AapSystem
import no.nav.aap.fordeler.arena.AvklarFordelingRepository
import no.nav.aap.fordeler.arena.AvklarFordelingVurdering
import no.nav.aap.fordeler.regler.RegelInput
import no.nav.aap.fordeler.ÅrsakTilStatus
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.BrukerIdType
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.gateway.hoveddokument
import no.nav.aap.postmottak.gateway.originalFiltype
import no.nav.aap.postmottak.journalpostCounter
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.steg.StegType
import org.slf4j.LoggerFactory
import java.time.LocalDateTime


class AvklarFordelingSteg(
    private val regelService: FordelerRegelService,
    private val journalpostService: JournalpostService,
    private val enhetsutreder: Enhetsutreder,
    private val avklarFordelingRepository: AvklarFordelingRepository,
    private val innkommendeJournalpostRepository: InnkommendeJournalpostRepository,
    private val gosysOppgaveGateway: GosysOppgaveGateway,
    private val prometheus: MeterRegistry = SimpleMeterRegistry(),
): BehandlingSteg {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return AvklarFordelingSteg(
                FordelerRegelService(repositoryProvider, gatewayProvider),
                JournalpostService.konstruer(repositoryProvider, gatewayProvider),
                Enhetsutreder.konstruer(gatewayProvider),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                gatewayProvider.provide(),
                PrometheusProvider.prometheus
            )
        }

        override fun type(): StegType {
            return StegType.AVKLAR_FORDELING
        }
    }

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val vurdering = avklarFordelingRepository.hentVurderingHvisEksisterer(kontekst.behandlingId)
        if (vurdering != null) {
            return Fullført
        }

        val statusMedÅrsakOgRegelresultat = evaluerDokument(kontekst)
        if (statusMedÅrsakOgRegelresultat.status == InnkommendeJournalpostStatus.EVALUERT) {
            // Hvis dokumentet allerede er lagret, vil status være IGNORERT med årsak ALLEREDE_JOURNALFØRT, derfor skjer dette kun 1 gang
            val safJournalpost = journalpostService.hentSafJournalpost(kontekst.journalpostId)
            innkommendeJournalpostRepository.lagre(
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
        }

        // På sikt kan regel-resultat være MANUELL_VURDERING: Da må vi ta høyde for dette. Vi skal kun lagre vurderingen automatisk hvis vi har
        // komplett automatisk vurdering her. For MANUELL_VURDERING må vi returnere FANT_AVKLARINGSBEHOV
        avklarFordelingRepository.lagreVurdering(kontekst.behandlingId, statusMedÅrsakOgRegelresultat.toFordelingVurdering(vurdertAv = "KELVIN"))
        return Fullført
    }

    private fun evaluerDokument(kontekst: FlytKontekst): StatusMedÅrsakOgRegelresultat {
        // TODO: Denne kan være problematisk hvis vi skal støtte at journalførte dokumenter skal kunne sendes inn til Kelvin
        if (innkommendeJournalpostRepository.eksisterer(kontekst.journalpostId)) {
            log.info("Journalposten med ID (${kontekst.journalpostId}) har allerede blitt evaluert - behandler ikke videre")
            return StatusMedÅrsakOgRegelresultat(
                InnkommendeJournalpostStatus.IGNORERT,
                ÅrsakTilStatus.ALLEREDE_JOURNALFØRT
            )
        }

        val safJournalpost = journalpostService.hentSafJournalpost(kontekst.journalpostId)

        return when {
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
                log.info("Evaluerer journalpost med ID ${journalpost.journalpostId}. Brevkode: ${journalpost.hoveddokumentbrevkode}.")
                val res = regelService.evaluer(
                    RegelInput(
                        safJournalpost.journalpostId,
                        journalpost.person,
                        journalpost.hoveddokumentbrevkode,
                        journalpost.mottattDato
                    )
                )
                StatusMedÅrsakOgRegelresultat(
                    InnkommendeJournalpostStatus.EVALUERT,
                    regelresultat = res
                )
            }
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
    ) {
        fun toFordelingVurdering(vurdertAv: String): AvklarFordelingVurdering {
            val system = if (status != InnkommendeJournalpostStatus.EVALUERT || regelresultat == null) {
                AapSystem.IGNORERT
            } else if(regelresultat.skalTilKelvin()) {
                AapSystem.KELVIN
            } else {
                AapSystem.ARENA
            }

            return AvklarFordelingVurdering(
                system = system,
                vurdertAv = vurdertAv,
                vurdertTidspunkt = LocalDateTime.now(),
            )
        }
    }
}
