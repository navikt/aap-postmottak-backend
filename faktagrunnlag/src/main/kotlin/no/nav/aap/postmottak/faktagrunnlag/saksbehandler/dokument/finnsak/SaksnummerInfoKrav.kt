package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytClient
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytKlient
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytSak
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.sakogbehandling.Ident

class SaksnummerInfoKrav(
    private val saksnummerRepository: SaksnummerRepository,
    private val behandlingsflytKlient: BehandlingsflytKlient,
    private val journalpostRepository: JournalpostRepository
) : Informasjonskrav {
    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): Informasjonskrav {
            return SaksnummerInfoKrav(
                SaksnummerRepository(connection),
                BehandlingsflytClient(),
                JournalpostRepositoryImpl(connection)
            )
        }
    }

    override fun oppdater(kontekst: FlytKontekst): Informasjonskrav.Endret {
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)
        requireNotNull(journalpost) { "Forventer journalpost" }

        val saker = try {
            behandlingsflytKlient.finnSaker(Ident(journalpost.person.aktivIdent().identifikator, true)).map { it.tilSaksinfo() }
        } catch (e: Exception) {
            emptyList()
        } // TODO utbedre exception handling!!!

        saksnummerRepository.lagreSaksnummer(kontekst.behandlingId, saker)
        return IKKE_ENDRET
    }

}

fun BehandlingsflytSak.tilSaksinfo(): Saksinfo {
    return Saksinfo(
        saksnummer, periode
    )
}