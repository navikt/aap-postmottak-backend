package no.nav.aap.postmottak.under18

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostInformasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.gateway.PersondataGateway
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst

class Under18FødselsdatoInformasjonskrav(
    private val journalpostRepository: JournalpostRepository,
    private val under18Repository: Under18Repository,
    private val persondataGateway: PersondataGateway,
) : Informasjonskrav {
    companion object : Informasjonskravkonstruktør {
        override val avhengigheter = setOf(JournalpostInformasjonskrav)

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ) = Under18FødselsdatoInformasjonskrav(
            repositoryProvider.provide(),
            repositoryProvider.provide(),
            gatewayProvider.provide(),
        )
    }

    override fun oppdater(kontekst: FlytKontekst): Informasjonskrav.Endret {
        val journalpost = checkNotNull(journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)) {
            "Journalpost er ikke lagret for behandling ${kontekst.behandlingId}"
        }
        val fødselsdato = checkNotNull(persondataGateway.hentFødselsdato(journalpost.person.aktivIdent().identifikator)) {
            "Fant ikke fødselsdato for søker på journalpost ${kontekst.journalpostId}"
        }
        val lagret = under18Repository.hentFødselsdato(kontekst.behandlingId)
        if (lagret == fødselsdato) {
            return IKKE_ENDRET
        }
        under18Repository.lagreFødselsdato(kontekst.behandlingId, fødselsdato)
        return if (lagret == null) IKKE_ENDRET else ENDRET
    }
}
