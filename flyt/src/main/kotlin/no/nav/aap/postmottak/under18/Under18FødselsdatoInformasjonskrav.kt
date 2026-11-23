package no.nav.aap.postmottak.under18

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravInput
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravRegisterdata
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostInformasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.gateway.Klokke
import no.nav.aap.postmottak.gateway.PersondataGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import java.time.LocalDate

class Under18FødselsdatoInformasjonskrav(
    private val journalpostRepository: JournalpostRepository,
    private val under18Repository: Under18Repository,
    private val persondataGateway: PersondataGateway,
    private val klokke: Klokke,
) : Informasjonskrav<Under18FødselsdatoInformasjonskrav.Input, Under18FødselsdatoInformasjonskrav.Registerdata> {
    data class Input(val ident: Ident, val vurderingsdato: LocalDate) : InformasjonskravInput

    data class Registerdata(val fødselsdato: LocalDate) : InformasjonskravRegisterdata

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.UNDER18_FOEDSELSDATO
        override val avhengigheter = setOf(JournalpostInformasjonskrav)

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ) = Under18FødselsdatoInformasjonskrav(
            repositoryProvider.provide(),
            repositoryProvider.provide(),
            gatewayProvider.provide(),
            gatewayProvider.provide(),
        )
    }

    override fun klargjør(kontekst: FlytKontekst): Input {
        val journalpost = checkNotNull(journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)) {
            "Journalpost er ikke lagret for behandling ${kontekst.behandlingId}"
        }
        return Input(journalpost.person.aktivIdent(), klokke.dagensDato())
    }

    override fun hentData(input: Input): Registerdata {
        val fødselsdato = checkNotNull(persondataGateway.hentFødselsdato(input.ident.identifikator)) {
            "Fant ikke fødselsdato for søker"
        }
        return Registerdata(fødselsdato)
    }

    override fun oppdater(input: Input, registerdata: Registerdata, kontekst: FlytKontekst): Informasjonskrav.Endret {
        val fødselsdato = registerdata.fødselsdato
        val lagretFødselsdato = under18Repository.hentFødselsdato(kontekst.behandlingId)
        val aldersgruppe = Under18Aldersgruppe.vurder(fødselsdato, input.vurderingsdato)
        val lagretAldersgruppe = under18Repository.hentAldersgruppe(kontekst.behandlingId)
        if (lagretFødselsdato == fødselsdato && lagretAldersgruppe == aldersgruppe) {
            return IKKE_ENDRET
        }
        under18Repository.lagreFødselsdato(kontekst.behandlingId, fødselsdato)
        under18Repository.lagreAldersgruppe(kontekst.behandlingId, aldersgruppe)
        return if (lagretFødselsdato == null) IKKE_ENDRET else ENDRET
    }
}
