package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.BehandlingsflytSak
import no.nav.aap.postmottak.gateway.Resultat
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(SaksnummerInfoKrav::class.java)

class SaksnummerInfoKrav(
    private val saksnummerRepository: SaksnummerRepository,
    private val behandlingsflytKlient: BehandlingsflytGateway,
    private val journalpostRepository: JournalpostRepository
) : Informasjonskrav {
    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): Informasjonskrav {
            val repositoryProvider = RepositoryProvider(connection)
            return SaksnummerInfoKrav(
                repositoryProvider.provide(SaksnummerRepository::class),
                GatewayProvider.provide(BehandlingsflytGateway::class),
                repositoryProvider.provide(JournalpostRepository::class)
            )
        }
    }

    override fun oppdater(kontekst: FlytKontekst): Informasjonskrav.Endret {
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)
        requireNotNull(journalpost) { "Forventer journalpost" }

        val saker =  behandlingsflytKlient.finnSaker(Ident(journalpost.person.aktivIdent().identifikator, true)).map { it.tilSaksinfo() }
       
        saksnummerRepository.lagreKelvinSak(kontekst.behandlingId, saker)
        return IKKE_ENDRET
    }

}

fun BehandlingsflytSak.tilSaksinfo(): Saksinfo {
    return Saksinfo(
        saksnummer, periode, resultat == Resultat.AVSLAG
    )
}