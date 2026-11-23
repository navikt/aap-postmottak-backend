package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak

import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravInput
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravRegisterdata
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.BehandlingsflytSaksInfoTilPostmottak
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst

class SaksnummerInfoKrav(
    private val saksnummerRepository: SaksnummerRepository,
    private val behandlingsflytKlient: BehandlingsflytGateway,
    private val journalpostRepository: JournalpostRepository
) : Informasjonskrav<SaksnummerInfoKrav.Input, SaksnummerInfoKrav.Registerdata> {
    data class Input(val ident: Ident) : InformasjonskravInput

    data class Registerdata(val saker: List<Saksinfo>) : InformasjonskravRegisterdata

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.SAKSNUMMER

        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): SaksnummerInfoKrav {
            return SaksnummerInfoKrav(
                repositoryProvider.provide(SaksnummerRepository::class),
                gatewayProvider.provide(BehandlingsflytGateway::class),
                repositoryProvider.provide(JournalpostRepository::class)
            )
        }
    }

    override fun klargjør(kontekst: FlytKontekst): Input {
        val journalpost =
            requireNotNull(journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)) { "Forventer journalpost for behandling ${kontekst.behandlingId}" }

        return Input(Ident(journalpost.person.aktivIdent().identifikator))
    }

    override fun hentData(input: Input) = Registerdata(
        behandlingsflytKlient.finnSaker(input.ident)
            .map { it.tilSaksinfo() }
    )

    override fun oppdater(input: Input, registerdata: Registerdata, kontekst: FlytKontekst): Informasjonskrav.Endret {
        val saker = registerdata.saker
        val eksisterendeSaker = saksnummerRepository.hentKelvinSaker(kontekst.behandlingId).toSet()

        if (saker.toSet() != eksisterendeSaker) {
            saksnummerRepository.lagreKelvinSak(kontekst.behandlingId, saker)
        }
        return IKKE_ENDRET
    }

}

fun BehandlingsflytSaksInfoTilPostmottak.tilSaksinfo(): Saksinfo {
    return Saksinfo(
        saksnummer = saksnummer,
        periode = periode,
        avslag = resultat == ResultatKode.AVSLAG,
        resultat = resultat,
        finnesÅpenBehandling = finnesÅpenBehandling,
        harRettNåEllerIFramtiden = harRettNåEllerIFramtiden
    )
}