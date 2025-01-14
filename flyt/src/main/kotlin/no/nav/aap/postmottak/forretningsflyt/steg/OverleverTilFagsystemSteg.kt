package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategoriVurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.søknad.parseDigitalSøknad
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.DokumentGateway
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(OverleverTilFagsystemSteg::class.java)

class OverleverTilFagsystemSteg(
    private val struktureringsvurderingRepository: StruktureringsvurderingRepository,
    private val kategorivurderingRepository: KategoriVurderingRepository,
    private val behandlingsflytKlient: BehandlingsflytGateway,
    private val journalpostRepository: JournalpostRepository,
    private val saksnummerRepository: SaksnummerRepository,
    private val dokumentGateway: DokumentGateway
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            return OverleverTilFagsystemSteg(
                repositoryProvider.provide(StruktureringsvurderingRepository::class),
                repositoryProvider.provide(KategoriVurderingRepository::class),
                GatewayProvider.provide(BehandlingsflytGateway::class),
                repositoryProvider.provide(JournalpostRepository::class),
                repositoryProvider.provide(SaksnummerRepository::class),
                GatewayProvider.provide(DokumentGateway::class)
            )
        }

        override fun type(): StegType {
            return StegType.OVERLEVER_TIL_FAGSYSTEM
        }
    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        val struktureringsvurdering =
            struktureringsvurderingRepository.hentStruktureringsavklaring(kontekst.behandlingId)
        val kategorivurdering = kategorivurderingRepository.hentKategoriAvklaring(kontekst.behandlingId)
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)
        require(journalpost != null)

        if (journalpost.erDigitalSøknad() || kategorivurdering?.avklaring == InnsendingType.SØKNAD) {
            // TODO :poop: bør kanskje gjøres på journalpost
            val dokumentJson =
                struktureringsvurdering?.vurdering?.parseDigitalSøknad()
                    ?: hentDokumentFraSaf(journalpost).parseDigitalSøknad()


            behandlingsflytKlient.sendHendelse(
                journalpost,
                InnsendingType.SØKNAD,
                saksnummerRepository.hentSakVurdering(kontekst.behandlingId)?.saksnummer!!,
                dokumentJson
            )

        } else if (journalpost.erDigitalLegeerklæring() || kategorivurdering?.avklaring == InnsendingType.LEGEERKLÆRING) {
            behandlingsflytKlient.sendHendelse(
                journalpost,
                InnsendingType.LEGEERKLÆRING,
                saksnummerRepository.hentSakVurdering(kontekst.behandlingId)?.saksnummer!!,
                null
            )
        } else {
            log.info("Dokument overleveres ikke til Fagsystem")
        }
        return Fullført
    }

    private fun hentDokumentFraSaf(journalpost: Journalpost): ByteArray {
        val strukturertDokument = journalpost.finnOriginal()
        requireNotNull(strukturertDokument) { "Finner ikke strukturert dokument" }
        return dokumentGateway.hentDokument(
            journalpost.journalpostId,
            strukturertDokument.dokumentInfoId
        ).dokument.readBytes()
    }
}