package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak

import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.BehandlingsflytSak
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst

class SaksnummerInfoKrav(
    private val saksnummerRepository: SaksnummerRepository,
    private val behandlingsflytKlient: BehandlingsflytGateway,
    private val journalpostRepository: JournalpostRepository
) : Informasjonskrav {
    companion object : Informasjonskravkonstruktør {
        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): Informasjonskrav {
            return SaksnummerInfoKrav(
                repositoryProvider.provide(SaksnummerRepository::class),
                gatewayProvider.provide(BehandlingsflytGateway::class),
                repositoryProvider.provide(JournalpostRepository::class)
            )
        }
    }

    override fun oppdater(kontekst: FlytKontekst): Informasjonskrav.Endret {
        val journalpost =
            requireNotNull(journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)) { "Forventer journalpost for behandling ${kontekst.behandlingId}" }


        val saker = behandlingsflytKlient.finnSaker(Ident(journalpost.person.aktivIdent().identifikator))
            .map { it.tilSaksinfo() }

        val eksisterendeSaker = saksnummerRepository.hentKelvinSaker(kontekst.behandlingId).toSet()

        if (saker.toSet() != eksisterendeSaker) {
            saksnummerRepository.lagreKelvinSak(kontekst.behandlingId, saker)
        }
        return IKKE_ENDRET
    }

}

fun BehandlingsflytSak.tilSaksinfo(): Saksinfo {
    return Saksinfo(
        saksnummer, periode, resultat == ResultatKode.AVSLAG
    )
}