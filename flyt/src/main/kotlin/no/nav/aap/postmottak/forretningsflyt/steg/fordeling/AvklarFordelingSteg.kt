package no.nav.aap.postmottak.forretningsflyt.steg.fordeling

import kotlinx.coroutines.runBlocking
import no.nav.aap.fordeler.FordelerRegelService
import no.nav.aap.fordeler.InnkommendeJournalpost
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.Regelresultat
import no.nav.aap.fordeler.arena.AapSystem
import no.nav.aap.fordeler.arena.ArenaService
import no.nav.aap.fordeler.arena.AvklarFordelingRepository
import no.nav.aap.fordeler.arena.AvklarFordelingVurdering
import no.nav.aap.fordeler.regler.RegelInput
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.gateway.BrukerIdType
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.gateway.hoveddokument
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway
import org.slf4j.LoggerFactory
import java.time.LocalDateTime


class AvklarFordelingSteg(
    private val regelService: FordelerRegelService,
    private val journalpostService: JournalpostService,
    private val avklarFordelingRepository: AvklarFordelingRepository,
    private val innkommendeJournalpostRepository: InnkommendeJournalpostRepository,
    private val arenaService: ArenaService,
    private val arenaoppslagGateway: ArenaoppslagGateway,
    private val unleashGateway: UnleashGateway,
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(javaClass)


    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return AvklarFordelingSteg(
                FordelerRegelService(repositoryProvider, gatewayProvider),
                JournalpostService.konstruer(repositoryProvider, gatewayProvider),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                ArenaService(gatewayProvider),
                gatewayProvider.provide(),
                gatewayProvider.provide(UnleashGateway::class),
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

        val innkommendeJournalpost = innkommendeJournalpostRepository.hentHvisEksisterer(kontekst.journalpostId)
        val safJournalpost = journalpostService.hentSafJournalpost(kontekst.journalpostId)

        if (innkommendeJournalpost == null) {
            // Innkommende journalpost lagres ikke for journalposter med orgnr som bruker. Eldre
            // fordelingsbehandlinger kan derfor stå her uten innkommende journalpost, og skal ignoreres.
            require(safJournalpost.bruker?.type == BrukerIdType.ORGNR) {
                "Journalposten skal allerede være lagret før dette steget kjører, men fant ikke innkommendeJournalpost for ${kontekst.journalpostId}"
            }
            log.info("Journalpost med id=${kontekst.journalpostId} er ikke lagret som innkommende journalpost: journalposten skal ignoreres siden den er knyttet til orgnummer. Behandler ikke videre.")
            avklarFordelingRepository.lagreVurdering(
                kontekst.behandlingId,
                AvklarFordelingVurdering(
                    system = AapSystem.IGNORERT,
                    vurdertAv = SYSTEMBRUKER.ident,
                    vurdertTidspunkt = LocalDateTime.now(),
                    kommentar = "Automatisk vurdert fordeling"
                )
            )
            return Fullført
        }

        val regelresultat = vurderFordelingRegler(kontekst, innkommendeJournalpost)

        val skalAvklaresManuelt =
            unleashGateway.isEnabled(PostmottakFeature.PostmottakManuellVurdering) &&
                    skalTilManuellVurdering(safJournalpost, kontekst)


        if (skalAvklaresManuelt) {
            log.info("Journalpost ${kontekst.journalpostId} sendes til manuell vurdering av fordeling")
            return FantAvklaringsbehov(Definisjon.AVKLAR_FORDELING)
        } else {
            val system = if(regelresultat.skalTilKelvin()) {
                AapSystem.KELVIN
            } else {
                AapSystem.ARENA
            }
            avklarFordelingRepository.lagreVurdering(
                kontekst.behandlingId,
                AvklarFordelingVurdering(
                    system = system,
                    vurdertAv = SYSTEMBRUKER.ident,
                    vurdertTidspunkt = LocalDateTime.now(),
                    kommentar = "Automatisk vurdert fordeling"
                )
            )
            return Fullført

        }
    }

    private fun skalTilManuellVurdering(safJournalpost: SafJournalpost, kontekst: FlytKontekst): Boolean {
        val brevkode = safJournalpost.hoveddokument()?.brevkode ?: return false

        if (brevkode != Brevkoder.SØKNAD.kode) {
            return false
        }
        val journalpost = journalpostService.tilJournalpostMedDokumentTitler(safJournalpost)

        return runBlocking {
            arenaService.skalManueltFordeles(
                søker = journalpost.person,
                mottattDato = journalpost.mottattDato,
                journalpostId = kontekst.journalpostId.referanse
            )
        }
    }

    private fun vurderFordelingRegler(kontekst: FlytKontekst, innkommendeJournalpost: InnkommendeJournalpost): Regelresultat {
        if (innkommendeJournalpost.regelresultat != null) {
            log.info("Journalposten med ID (${kontekst.journalpostId}) har allerede blitt evaluert - behandler ikke videre")
            return innkommendeJournalpost.regelresultat
        }

        val safJournalpost = journalpostService.hentSafJournalpost(kontekst.journalpostId)
        val journalpost = journalpostService.tilJournalpostMedDokumentTitler(safJournalpost)

        val res = regelService.evaluer(
            RegelInput(
                safJournalpost.journalpostId,
                journalpost.person,
                journalpost.hoveddokumentbrevkode,
                journalpost.mottattDato
            )
        )

        innkommendeJournalpostRepository.update(innkommendeJournalpost.copy(regelresultat = res))
        log.info("Evaluerte journalpost med ID ${journalpost.journalpostId}. Brevkode: ${journalpost.hoveddokumentbrevkode}. Fordeles til: ${res.systemNavn}")
        return res
    }
}
