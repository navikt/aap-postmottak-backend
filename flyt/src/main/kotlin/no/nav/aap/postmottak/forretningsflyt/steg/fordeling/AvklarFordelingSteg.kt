package no.nav.aap.postmottak.forretningsflyt.steg.fordeling

import no.nav.aap.fordeler.FordelerRegelService
import no.nav.aap.fordeler.InnkommendeJournalpost
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.Regelresultat
import no.nav.aap.fordeler.arena.AapSystem
import no.nav.aap.fordeler.arena.AvklarFordelingRepository
import no.nav.aap.fordeler.arena.AvklarFordelingVurdering
import no.nav.aap.fordeler.regler.RegelInput
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.BrukerIdType
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.steg.StegType
import org.slf4j.LoggerFactory
import java.time.LocalDateTime


class AvklarFordelingSteg(
    private val regelService: FordelerRegelService,
    private val journalpostService: JournalpostService,
    private val avklarFordelingRepository: AvklarFordelingRepository,
    private val innkommendeJournalpostRepository: InnkommendeJournalpostRepository,
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

        if (innkommendeJournalpost == null) {
            val safJournalpost = journalpostService.hentSafJournalpost(kontekst.journalpostId)
            // Innkommende journalpost lagres ikke for journalposter med orgnr som bruker. Eldre
            // fordelingsbehandlinger kan derfor stå her uten innkommende journalpost, og skal ignoreres.
            require(safJournalpost.bruker?.type == BrukerIdType.ORGNR) {
                "Journalposten skal allerede være lagret før dette steget kjører, men fant ikke innkommendeJournalpost for ${kontekst.journalpostId}"
            }
            log.info("Journalpost med id=${kontekst.journalpostId} er ikke lagret som innkommende journalpost: journalposten skal ignoreres siden den er knyttet til orgnummer. Behandler ikke videre.")
            lagreAutomatiskVurdering(kontekst, AapSystem.IGNORERT)
            return Fullført
        }

        val regelresultat = vurderFordelingRegler(kontekst, innkommendeJournalpost)
        val system = if (regelresultat.skalTilKelvin()) AapSystem.KELVIN else AapSystem.ARENA
        lagreAutomatiskVurdering(kontekst, system)
        return Fullført
    }

    private fun lagreAutomatiskVurdering(kontekst: FlytKontekst, system: AapSystem) {
        avklarFordelingRepository.lagreVurdering(
            kontekst.behandlingId,
            AvklarFordelingVurdering(
                system = system,
                vurdertAv = SYSTEMBRUKER.ident,
                vurdertTidspunkt = LocalDateTime.now(),
                kommentar = "Automatisk vurdert fordeling"
            )
        )
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
