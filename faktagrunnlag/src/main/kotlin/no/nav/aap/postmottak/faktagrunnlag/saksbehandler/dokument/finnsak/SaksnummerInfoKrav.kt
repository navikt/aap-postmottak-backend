package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.faktagrunnlag.register.behandlingsflyt.BehandlingsflytClient
import no.nav.aap.postmottak.faktagrunnlag.register.behandlingsflyt.BehandlingsflytGateway
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.klient.joark.Journalpost
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.sakogbehandling.Ident

class SaksnummerInfoKrav(
    private val saksnummerRepository: SaksnummerRepository,
    private val behandlingsflytGateway: BehandlingsflytGateway,
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
        check(journalpost is Journalpost.MedIdent) { "journalpost må ha ident" }

        val saksnummre = try {
            behandlingsflytGateway.finnSaker(Ident(journalpost.personident.id, true))
        } catch (e: Exception) {
            emptyList()
        } // TODO utbedre exception handling!!!

        saksnummerRepository.lagreSaksnummer(kontekst.behandlingId, saksnummre)
        return IKKE_ENDRET
    }

}