package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever

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
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.Digitaliseringsvurdering
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.DokumentTilMeldingParser
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

class DokumentOverleveringService(private val behandlingsflytGateway: BehandlingsflytGateway) {
    fun overlever(
        journalpostId: JournalpostId,
        kildejournalpost: Journalpost,
        digitaliseringsvurdering: Digitaliseringsvurdering,
        saksnummer: String,
        begrunnelse: String?
    ) {
        val melding = utledMelding(digitaliseringsvurdering, begrunnelse)
        behandlingsflytGateway.sendHendelse(
            journalpostId = journalpostId,
            kanal = kildejournalpost.kanal,
            mottattDato = digitaliseringsvurdering.søknadsdato?.atStartOfDay()
                ?: kildejournalpost.mottattTid
                ?: kildejournalpost.mottattDato.atStartOfDay(),
            innsendingstype = digitaliseringsvurdering.kategori,
            saksnummer = saksnummer,
            melding = melding,
            digitalisertIPostmottak = digitaliseringsvurdering.digitalisertManueltGjennomPostmottak ?: false
        )
    }

    private fun utledMelding(
        digitaliseringsvurdering: Digitaliseringsvurdering,
        begrunnelse: String?
    ): Melding? {
        return when {
            digitaliseringsvurdering.kategori == InnsendingType.LEGEERKLÆRING -> LegeerklæringV0(
                beskrivelse = begrunnelse
            )

            else -> when (val melding = DokumentTilMeldingParser.parseTilMelding(
                digitaliseringsvurdering.strukturertDokument,
                digitaliseringsvurdering.kategori
            )) {
                is AnnetRelevantDokument -> {
                    if (melding.begrunnelse.isNullOrBlank() && begrunnelse != null) when (melding) {
                        is AnnetRelevantDokumentV0 -> AnnetRelevantDokumentV1(
                            årsakerTilBehandling = melding.årsakerTilBehandling,
                            begrunnelse = begrunnelse,
                            underkategori = null
                        )

                        is AnnetRelevantDokumentV1 -> melding.copy(begrunnelse = begrunnelse)
                    } else melding
                }

                is LegeerklæringV0 -> error("Skal ikke kunne skje, LegeerklæringV0 blir kun konstruert over.")
                is KlageV0 -> {
                    if (melding.beskrivelse.isBlank() && begrunnelse != null) {
                        melding.copy(beskrivelse = begrunnelse)
                    } else melding
                }

                is MeldekortV0 -> {
                    if (melding.begrunnelse.isNullOrBlank() && begrunnelse != null) {
                        melding.copy(begrunnelse = begrunnelse)
                    } else melding
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
                is UførevedtakV0 -> melding

                null -> null
            }
        }
    }
}
