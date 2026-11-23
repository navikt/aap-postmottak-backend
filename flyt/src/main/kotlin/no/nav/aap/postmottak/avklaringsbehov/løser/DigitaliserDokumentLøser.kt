package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løsning.DigitaliserDokumentLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.DigitaliseringsvurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.ManuellDigitalisering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

class DigitaliserDokumentLøser(
    val struktureringsvurderingRepository: DigitaliseringsvurderingRepository,
    val journalpostRepository: JournalpostRepository,
    val sakVurderingRepository: SaksnummerRepository,
) : AvklaringsbehovsLøser<DigitaliserDokumentLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: DigitaliserDokumentLøsning): LøsningsResultat {
        val journalpost = requireNotNull(journalpostRepository.hentHvisEksisterer(kontekst.kontekst.journalpostId))
        val behandlingId = kontekst.kontekst.behandlingId

        if (journalpost.erDigitalSøknad() && løsning.kategori != InnsendingType.SØKNAD) {
            throw UgyldigForespørselException("Digital søknad kan ikke kategoriseres som noe annet enn søknad.")
            
        }

        val digitaliseringsvurdering = ManuellDigitalisering.lagVurdering(
            journalpost = journalpost,
            kategori = løsning.kategori,
            strukturertDokument = løsning.strukturertDokument,
            søknadsdato = løsning.søknadsdato,
            saksvurdering = sakVurderingRepository.hentSakVurdering(behandlingId)
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

    companion object : LøserKonstruktør<DigitaliserDokumentLøsning> {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): AvklaringsbehovsLøser<DigitaliserDokumentLøsning> {
            return DigitaliserDokumentLøser(
                repositoryProvider.provide<DigitaliseringsvurderingRepository>(),
                repositoryProvider.provide<JournalpostRepository>(),
                repositoryProvider.provide<SaksnummerRepository>(),
            )
        }
    }
}
