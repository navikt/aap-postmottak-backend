package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageV0
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løsning.DigitaliserDokumentLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.Digitaliseringsvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.DigitaliseringsvurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.gateway.DokumentTilMeldingParser
import no.nav.aap.postmottak.gateway.serialiser
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

class DigitaliserDokumentLøser(
    val struktureringsvurderingRepository: DigitaliseringsvurderingRepository,
    val journalpostRepository: JournalpostRepository,
    val sakVurderingRepository: SaksnummerRepository,
    val overleveringVurderingRepository: OverleveringVurderingRepository,
) : AvklaringsbehovsLøser<DigitaliserDokumentLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: DigitaliserDokumentLøsning): LøsningsResultat {
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.kontekst.journalpostId)!!
        require((løsning.søknadsdato == null) xor (løsning.kategori == InnsendingType.SØKNAD)) {
            "Søknadsdato skal kun settes for søknader"
        }
        require(løsning.søknadsdato == null || !løsning.søknadsdato.isAfter(journalpost.mottattDato)) {
            "Søknadsdato kan ikke være etter registrert dato"
        }

        val behandlingId = kontekst.kontekst.behandlingId
        val avklartSak = sakVurderingRepository.hentSakVurdering(behandlingId)
        require(!(løsning.kategori == InnsendingType.KLAGE && avklartSak?.opprettetNy!!)) {
            "Klage skal knyttes mot eksisterende sak"
        }

        val dokument = DokumentTilMeldingParser.parseTilMelding(løsning.strukturertDokument, løsning.kategori)

        val digitaliseringsvurdering = Digitaliseringsvurdering(
            kategori = løsning.kategori,
            strukturertDokument = dokument?.serialiser(),
            søknadsdato = løsning.søknadsdato
        )
        struktureringsvurderingRepository.lagre(
            behandlingId,
            digitaliseringsvurdering
        )
        if (dokument is KlageV0) {
            val overleveringVurdering = OverleveringVurdering(
                skalOverleveresTilKelvin = dokument.skalOppretteNyBehandling == true
            )
            overleveringVurderingRepository.lagre(behandlingId, overleveringVurdering)
        }

        return LøsningsResultat("Dokument er kategorisert og digitalisert")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.DIGITALISER_DOKUMENT
    }

    companion object : LøserKonstruktør<DigitaliserDokumentLøsning> {
        override fun konstruer(connection: DBConnection): AvklaringsbehovsLøser<DigitaliserDokumentLøsning> {
            val repositoryProvider = RepositoryProvider(connection)

            return DigitaliserDokumentLøser(
                repositoryProvider.provide(DigitaliseringsvurderingRepository::class),
                repositoryProvider.provide(JournalpostRepository::class),
                repositoryProvider.provide(SaksnummerRepository::class),
                repositoryProvider.provide(OverleveringVurderingRepository::class),
            )
        }
    }
}
