package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løsning.DigitaliserDokumentLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.Digitaliseringsvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.DigitaliseringsvurderingRepository
import no.nav.aap.postmottak.gateway.DokumentTilMeldingParser
import no.nav.aap.postmottak.gateway.serialiser
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

class DigitaliserDokumentLøser(val connection: DBConnection) : AvklaringsbehovsLøser<DigitaliserDokumentLøsning> {
    val repositoryProvider = RepositoryProvider(connection)
    val struktureringsvurderingRepository = repositoryProvider.provide(DigitaliseringsvurderingRepository::class)
    val journalpostRepository = repositoryProvider.provide(JournalpostRepository::class)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: DigitaliserDokumentLøsning): LøsningsResultat {
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.kontekst.journalpostId)!!
        require((løsning.søknadsdato == null) xor (løsning.kategori == InnsendingType.SØKNAD)) {
            "Søknadsdato skal kun settes for søknader"
        }
        require(løsning.søknadsdato == null || !løsning.søknadsdato.isAfter(journalpost.mottattDato)) {
            "Søknadsdato kan ikke være etter registrert dato"
        }

        val dokument =
            DokumentTilMeldingParser.parseTilMelding(løsning.strukturertDokument, løsning.kategori)?.serialiser()

        struktureringsvurderingRepository.lagre(
            kontekst.kontekst.behandlingId, Digitaliseringsvurdering(løsning.kategori, dokument, løsning.søknadsdato)
        )

        return LøsningsResultat("Dokument er kategorisert og digitalisert")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.DIGITALISER_DOKUMENT
    }
}
