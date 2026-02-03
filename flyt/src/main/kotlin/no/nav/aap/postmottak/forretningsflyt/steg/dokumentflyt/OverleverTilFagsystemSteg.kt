package no.nav.aap.postmottak.forretningsflyt.steg.dokumentflyt

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.DigitaliseringsvurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.DokumentTilMeldingParser
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(OverleverTilFagsystemSteg::class.java)

class OverleverTilFagsystemSteg(
    private val digitaliseringsvurderingRepository: DigitaliseringsvurderingRepository,
    private val behandlingsflytKlient: BehandlingsflytGateway,
    private val journalpostRepository: JournalpostRepository,
    private val saksnummerRepository: SaksnummerRepository,
    private val overleveringVurderingRepository: OverleveringVurderingRepository,
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return OverleverTilFagsystemSteg(
                repositoryProvider.provide(DigitaliseringsvurderingRepository::class),
                gatewayProvider.provide(BehandlingsflytGateway::class),
                repositoryProvider.provide(JournalpostRepository::class),
                repositoryProvider.provide(SaksnummerRepository::class),
                repositoryProvider.provide(OverleveringVurderingRepository::class)
            )
        }

        override fun type(): StegType {
            return StegType.OVERLEVER_TIL_FAGSYSTEM
        }
    }

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val digitaliseringsvurdering =
            requireNotNull(digitaliseringsvurderingRepository.hentHvisEksisterer(kontekst.behandlingId)) { "Digitaliseringsvurdering mangler for behandlingID ${kontekst.behandlingId} i OverleverTilFagsystemSteg" }
        val journalpost =
            requireNotNull(journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)) { "Fant ikke journalpost for behandlingID ${kontekst.behandlingId} i OverleverTilFagsystemSteg" }

        var overleveringVurdering = overleveringVurderingRepository.hentHvisEksisterer(kontekst.behandlingId)

        if (overleveringVurdering == null && digitaliseringsvurdering.kategori in setOf(
                InnsendingType.SØKNAD,
                InnsendingType.LEGEERKLÆRING,
                InnsendingType.MELDEKORT,
                InnsendingType.KLAGE
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
                    digitaliseringsvurdering.strukturertDokument,
                    digitaliseringsvurdering.kategori
                )
                behandlingsflytKlient.sendHendelse(
                    journalpostId = journalpost.journalpostId,
                    kanal = journalpost.kanal,
                    mottattDato = digitaliseringsvurdering.søknadsdato?.atStartOfDay()
                        ?: journalpost.mottattTid
                        ?: journalpost.mottattDato.atStartOfDay(),
                    innsendingstype = digitaliseringsvurdering.kategori,
                    saksnummer = saksnummerRepository.hentSakVurdering(kontekst.behandlingId)?.saksnummer!!,
                    melding = melding,
                    digitalisertIPostmottak = digitaliseringsvurdering.digitalisertManueltGjennomPostmottak ?: false
                )
            }
            return Fullført
        }
    }
}