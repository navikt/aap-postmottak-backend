package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.joark.Joark
import no.nav.aap.behandlingsflyt.joark.JoarkClient
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class JournalføringSteg(
    private val journalpostRepository: JournalpostRepository,
    private val joarkKlient: Joark
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return JournalføringSteg(
                JournalpostRepositoryImpl(connection),
                JoarkClient()
            )
        }

        override fun type(): StegType {
            return StegType.ENDELIG_JOURNALFØRING
        }
    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)

        require(journalpost is Journalpost.MedIdent)

        joarkKlient.ferdigstillJournalpost(journalpost)

        return StegResultat()
    }
}