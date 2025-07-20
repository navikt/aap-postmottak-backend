package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løsning.DigitaliserDokumentLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.Digitaliseringsvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.DigitaliseringsvurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.gateway.DokumentTilMeldingParser
import no.nav.aap.postmottak.gateway.serialiser
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

class DigitaliserDokumentLøser(val connection: DBConnection) : AvklaringsbehovsLøser<DigitaliserDokumentLøsning> {
    val repositoryProvider = RepositoryProvider(connection)
    val struktureringsvurderingRepository = repositoryProvider.provide(DigitaliseringsvurderingRepository::class)
    val journalpostRepository = repositoryProvider.provide(JournalpostRepository::class)
    val sakVurderingRepository = repositoryProvider.provide(SaksnummerRepository::class)
    val overleveringVurderingRepository = repositoryProvider.provide(OverleveringVurderingRepository::class)

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

        return LøsningsResultat("Dokument er kategorisert og digitalisert")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.DIGITALISER_DOKUMENT
    }
}
