package no.nav.aap.postmottak.forretningsflyt.steg.dokumentflyt

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Aktivitetskort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AnnetRelevantDokument
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AnnetRelevantDokumentV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AnnetRelevantDokumentV1
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ForeldrepengeVedtakV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.InstitusjonsOppholdHendelseV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelseV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KorrigerSøknadsdatoV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.LegeerklæringV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MigreringFraArenaV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandlingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OmgjøringKlageRevurdering
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppfølgingsoppgaveV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.PdlHendelseV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SykepengevedtakV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Søknad
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.Digitaliseringsvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.DigitaliseringsvurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.tillaterAutomatiskBehandlingAvLegeerklæring
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
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway
import org.slf4j.LoggerFactory

class OverleverTilFagsystemSteg(
    private val digitaliseringsvurderingRepository: DigitaliseringsvurderingRepository,
    private val behandlingsflytKlient: BehandlingsflytGateway,
    private val journalpostRepository: JournalpostRepository,
    private val saksnummerRepository: SaksnummerRepository,
    private val overleveringVurderingRepository: OverleveringVurderingRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val unleashGateway: UnleashGateway,
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(javaClass)

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
                repositoryProvider.provide(OverleveringVurderingRepository::class),
                repositoryProvider.provide(AvklaringsbehovRepository::class),
                gatewayProvider.provide(UnleashGateway::class)
            )
        }

        override fun type(): StegType {
            return StegType.OVERLEVER_TIL_FAGSYSTEM
        }
    }

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val journalpost =
            requireNotNull(journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)) { "Fant ikke journalpost for behandlingID ${kontekst.behandlingId} i OverleverTilFagsystemSteg" }

        if (journalpost.erUgyldig()) {
            log.warn("Journalposten er ugyldig - dokumentet kan derfor ikke digitaliseres.  JournalpostId: ${journalpost.journalpostId} Status: ${journalpost.status}")
            avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                .avbrytForSteg(StegType.DIGITALISER_DOKUMENT)
            return Fullført
        }

        val digitaliseringsvurdering =
            requireNotNull(digitaliseringsvurderingRepository.hentHvisEksisterer(kontekst.behandlingId)) { "Digitaliseringsvurdering mangler for behandlingID ${kontekst.behandlingId} i OverleverTilFagsystemSteg" }

        val tillaterAutomatiskLegeerklæring by lazy {
            !unleashGateway.isEnabled(PostmottakFeature.StoppAutomatikkForLegeerklaringVedAvslag)
                    || saksnummerRepository.hentKelvinSaker(kontekst.behandlingId)
                .tillaterAutomatiskBehandlingAvLegeerklæring()
        }

        var overleveringVurdering = overleveringVurderingRepository.hentHvisEksisterer(kontekst.behandlingId)

        if (overleveringVurdering == null && digitaliseringsvurdering.kategori in setOf(
                InnsendingType.SØKNAD,
                InnsendingType.LEGEERKLÆRING,
                InnsendingType.MELDEKORT,
                InnsendingType.KLAGE
            ) && (digitaliseringsvurdering.kategori != InnsendingType.LEGEERKLÆRING || tillaterAutomatiskLegeerklæring)
        ) {
            val skalOverleveresTilKelvin = when {
                // Meldekort uten strukturert dokument skal ikke oversendes fagsystem da dette allerede er registrert manuelt i Kelvin
                digitaliseringsvurdering.kategori == InnsendingType.MELDEKORT && digitaliseringsvurdering.strukturertDokument == null -> false
                else -> true
            }

            val vurdering = OverleveringVurdering(skalOverleveresTilKelvin, begrunnelse = null)
            overleveringVurderingRepository.lagre(kontekst.behandlingId, vurdering)
            overleveringVurdering = vurdering
        }

        if (overleveringVurdering == null) {
            return FantAvklaringsbehov(Definisjon.AVKLAR_OVERLEVERING)
        } else {
            log.info("Dokument overleveres${if (overleveringVurdering.skalOverleveresTilKelvin) " " else "ikke"} til Fagsystem")
            if (overleveringVurdering.skalOverleveresTilKelvin) {
                val melding = utledMelding(digitaliseringsvurdering, overleveringVurdering)
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

    fun utledMelding(
        digitaliseringsvurdering: Digitaliseringsvurdering,
        overleveringVurdering: OverleveringVurdering
    ): Melding? {
        return when {
            digitaliseringsvurdering.kategori == InnsendingType.LEGEERKLÆRING -> LegeerklæringV0(
                beskrivelse = overleveringVurdering.begrunnelse
            )

            else -> DokumentTilMeldingParser.parseTilMelding(
                digitaliseringsvurdering.strukturertDokument,
                digitaliseringsvurdering.kategori
            )?.let {
                when (it) {
                    is AnnetRelevantDokument -> {
                        if (it.begrunnelse.isNullOrBlank() && overleveringVurdering.begrunnelse != null) when (it) {
                            is AnnetRelevantDokumentV0 -> AnnetRelevantDokumentV1(
                                årsakerTilBehandling = it.årsakerTilBehandling,
                                begrunnelse = overleveringVurdering.begrunnelse,
                                underkategori = null
                            )

                            is AnnetRelevantDokumentV1 -> it.copy(begrunnelse = overleveringVurdering.begrunnelse)
                        } else it
                    }

                    is LegeerklæringV0 -> error("Skal ikke kunne skje, LegeerklæringV0 blir kun konstruert over.")
                    is KlageV0 -> {
                        if (it.beskrivelse.isBlank() && overleveringVurdering.begrunnelse != null) {
                            it.copy(beskrivelse = overleveringVurdering.begrunnelse)
                        } else it
                    }

                    is MeldekortV0 -> {
                        if (it.begrunnelse.isNullOrBlank() && overleveringVurdering.begrunnelse != null) {
                            it.copy(begrunnelse = overleveringVurdering.begrunnelse)
                        } else it
                    }

                    // Disse har ikke begrunnelse / blir ikke digitalisert i postmottak
                    is ManuellRevurderingV0,
                    is Aktivitetskort,
                    is ForeldrepengeVedtakV0,
                    is InstitusjonsOppholdHendelseV0,
                    is KabalHendelseV0,
                    is KorrigerSøknadsdatoV0,
                    is MigreringFraArenaV0,
                    is NyÅrsakTilBehandlingV0,
                    is OmgjøringKlageRevurdering,
                    is OppfølgingsoppgaveV0,
                    is PdlHendelseV0,
                    is SykepengevedtakV0,
                    is Søknad,
                    is TilbakekrevingHendelse,
                    is UførevedtakV0 -> it
                }
            }
        }
    }
}
