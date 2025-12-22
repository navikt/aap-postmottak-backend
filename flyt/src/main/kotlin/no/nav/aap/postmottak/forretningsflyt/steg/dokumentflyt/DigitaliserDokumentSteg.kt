package no.nav.aap.postmottak.forretningsflyt.steg.dokumentflyt

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DeserializationException
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.avklaringsbehov.AvslagException
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.Digitaliseringsvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.DigitaliseringsvurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.DokumentGateway
import no.nav.aap.postmottak.gateway.DokumentTilMeldingParser
import no.nav.aap.postmottak.gateway.serialiser
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.BrevkoderHelper
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import org.slf4j.LoggerFactory


class DigitaliserDokumentSteg(
    private val digitaliseringsvurderingRepository: DigitaliseringsvurderingRepository,
    private val journalpostRepository: JournalpostRepository,
    private val dokumentGateway: DokumentGateway,
    private val saksnummerRepository: SaksnummerRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) : BehandlingSteg {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return DigitaliserDokumentSteg(
                repositoryProvider.provide(DigitaliseringsvurderingRepository::class),
                repositoryProvider.provide(JournalpostRepository::class),
                gatewayProvider.provide(DokumentGateway::class),
                repositoryProvider.provide(SaksnummerRepository::class),
                repositoryProvider.provide(AvklaringsbehovRepository::class)
            )
        }

        override fun type(): StegType {
            return StegType.DIGITALISER_DOKUMENT
        }
    }

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val struktureringsvurdering =
            digitaliseringsvurderingRepository.hentHvisEksisterer(kontekst.behandlingId)

        if (struktureringsvurdering != null) {
            avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId).avbrytForSteg(StegType.DIGITALISER_DOKUMENT)
            return Fullført
        }

        val journalpost =
            requireNotNull(journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)) { "Fant ikke journalpost for behandlingID ${kontekst.behandlingId}" }

        if (saksnummerRepository.eksistererAvslagPåTidligereBehandling(kontekst.behandlingId)) throw AvslagException()

        // Prøv automatisk digitalisering av dokumenter som er digitale
        if (journalpost.erDigitalSøknad() || journalpost.erDigitalLegeerklæring() || journalpost.erDigitaltMeldekort()) {
            val dokument =
                if (journalpost.erDigitalSøknad() || journalpost.erDigitaltMeldekort()) {
                    hentOriginalDokumentFraSaf(journalpost)
                } else null
            val innsendingType = BrevkoderHelper.mapTilInnsendingType(journalpost.hoveddokumentbrevkode)

            log.info("Parser dokument for behandling ${kontekst.behandlingId}. Innsendingtype: $innsendingType.")

            val logFeil = { msg: String? ->
                log.warn("Dokument kunne ikke valideres, oppretter avklaringsbehov for manuell digitalisering. Behandling: ${kontekst.behandlingId}, innsendingtype: $innsendingType, error: ${msg}")
            }
            val validertDokument = try {
                DokumentTilMeldingParser.parseTilMelding(
                    dokument,
                    requireNotNull(innsendingType) { "Innsendingtype kan ikke være null." })?.serialiser()
            } catch (e: DeserializationException) {
                logFeil(e.message)
                return FantAvklaringsbehov(Definisjon.DIGITALISER_DOKUMENT)
            } catch (e: IllegalArgumentException) {
                logFeil(e.message)
                return FantAvklaringsbehov(Definisjon.DIGITALISER_DOKUMENT)
            }
            digitaliseringsvurderingRepository.lagre(
                kontekst.behandlingId, Digitaliseringsvurdering(innsendingType, validertDokument, null)
            )

            return Fullført
        }

        return FantAvklaringsbehov(Definisjon.DIGITALISER_DOKUMENT)
    }

    private fun hentOriginalDokumentFraSaf(journalpost: Journalpost): ByteArray {
        val strukturertDokument = journalpost.finnOriginal()
        requireNotNull(strukturertDokument) { "Finner ikke strukturert dokument for journalpostId ${journalpost.journalpostId}" }
        return dokumentGateway.hentDokument(
            journalpost.journalpostId,
            strukturertDokument.dokumentInfoId
        ).dokument.readBytes()
    }
}
