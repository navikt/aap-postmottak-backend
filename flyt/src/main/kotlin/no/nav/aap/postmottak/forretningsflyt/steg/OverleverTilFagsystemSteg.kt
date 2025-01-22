package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategoriVurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.DokumentTilMeldingParser
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(OverleverTilFagsystemSteg::class.java)

class OverleverTilFagsystemSteg(
    private val struktureringsvurderingRepository: StruktureringsvurderingRepository,
    private val kategorivurderingRepository: KategoriVurderingRepository,
    private val behandlingsflytKlient: BehandlingsflytGateway,
    private val journalpostRepository: JournalpostRepository,
    private val saksnummerRepository: SaksnummerRepository,
    private val overleveringVurderingRepository: OverleveringVurderingRepository,
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
                repositoryProvider.provide(OverleveringVurderingRepository::class)
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

        var overleveringVurdering = overleveringVurderingRepository.hentHvisEksisterer(kontekst.behandlingId)

        if (overleveringVurdering == null && kategorivurdering.avklaring in setOf(
                InnsendingType.SØKNAD,
                InnsendingType.LEGEERKLÆRING
            )
        ) {
            val vurdering = OverleveringVurdering(true)
            overleveringVurderingRepository.lagre(kontekst.behandlingId, OverleveringVurdering(true))
            overleveringVurdering = vurdering
        }

        if (overleveringVurdering == null) {
            return FantAvklaringsbehov(Definisjon.AVKLAR_OVERLEVERING)
        } else {
            log.info("Dokument overleveres${if (overleveringVurdering.skalOverleveresTilKelvin) " " else "ikke"}til Fagsystem")
            if (overleveringVurdering.skalOverleveresTilKelvin) {
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
            }
            return Fullført
        }
    }
}