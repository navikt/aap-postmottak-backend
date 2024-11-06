package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytClient
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytGateway
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.klient.saf.SafRestClient
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategorivurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.søknad.berik
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.søknad.parseDigitalSøknad
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.postmottak.sakogbehandling.journalpost.Journalpost
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(OverleverTilFagsystemSteg::class.java)

class OverleverTilFagsystemSteg(
    private val struktureringsvurderingRepository: StruktureringsvurderingRepository,
    private val kategorivurderingRepository: KategorivurderingRepository,
    private val behandlingsflytGateway: BehandlingsflytGateway,
    private val journalpostRepository: JournalpostRepository,
    private val saksnummerRepository: SaksnummerRepository,
    private val safRestClient: SafRestClient
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return OverleverTilFagsystemSteg(
                StruktureringsvurderingRepository(connection),
                KategorivurderingRepository(connection),
                BehandlingsflytClient(),
                JournalpostRepositoryImpl(connection),
                saksnummerRepository = SaksnummerRepository(connection),
                SafRestClient.withClientCredentialsRestClient()
            )
        }

        override fun type(): StegType {
            return StegType.OVERLEVER_TIL_FAGSYSTEM
        }
    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        val struktureringsvurdering = struktureringsvurderingRepository.hentStruktureringsavklaring(kontekst.behandlingId)
        val kategorivurdering = kategorivurderingRepository.hentKategoriAvklaring(kontekst.behandlingId)
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)
        require(journalpost != null)

        if (!journalpost.erSøknad() && kategorivurdering?.avklaring != Brevkode.SØKNAD) {
            log.info("Dokument er ikke en søknad, og skal ikke sendes til fagsystem")
            return Fullført
        }

        // TODO :poop: bør kanskje gjøres på journalpost
        val dokumentJson =
            struktureringsvurdering?.vurdering?.parseDigitalSøknad()?.berik()
                ?: hentDokumentFraSaf(journalpost).parseDigitalSøknad().berik()

        behandlingsflytGateway.sendSøknad(
            saksnummerRepository.hentSakVurdering(kontekst.behandlingId)?.saksnummer!!,
            journalpost.journalpostId,
            dokumentJson
        )

        return Fullført
    }

    private fun hentDokumentFraSaf(journalpost: Journalpost): ByteArray {
        val strukturertDokument = journalpost.finnOriginal()
        requireNotNull(strukturertDokument) { "Finner ikke strukturert dokument" }
        return safRestClient.hentDokument(
            journalpost.journalpostId,
            strukturertDokument.dokumentInfoId
        ).dokument.readBytes()
    }
}