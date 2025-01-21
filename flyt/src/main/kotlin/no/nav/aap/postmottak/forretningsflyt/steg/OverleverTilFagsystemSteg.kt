package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategoriVurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.DokumentTilMeldingParser
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.kontrakt.steg.StegType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(OverleverTilFagsystemSteg::class.java)

class OverleverTilFagsystemSteg(
    private val struktureringsvurderingRepository: StruktureringsvurderingRepository,
    private val kategorivurderingRepository: KategoriVurderingRepository,
    private val behandlingsflytKlient: BehandlingsflytGateway,
    private val journalpostRepository: JournalpostRepository,
    private val saksnummerRepository: SaksnummerRepository,
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
        requireNotNull(journalpost) { "Journalpost mangler i OverleverTilFagsystemSteg" }
        requireNotNull(kategorivurdering) { "Kategorivurdering mangler i OverleverTilFagsystemSteg" }

        if (skalSendesTilBehandlingsflyt(kategorivurdering.avklaring)) {
            val melding = DokumentTilMeldingParser.parseTilMelding(
                struktureringsvurdering?.vurdering,
                kategorivurdering.avklaring
            )
            behandlingsflytKlient.sendHendelse(
                journalpost,
                kategorivurdering.avklaring,
                saksnummerRepository.hentSakVurdering(kontekst.behandlingId)?.saksnummer!!,
                melding
            )
            return Fullført
        }

        log.info("Dokument overleveres ikke til Fagsystem")
        return Fullført
    }

    private fun skalSendesTilBehandlingsflyt(innsendingstype: InnsendingType): Boolean {
        return innsendingstype in setOf(InnsendingType.SØKNAD, InnsendingType.LEGEERKLÆRING)
    }
}