package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.klient.joark.Joark
import no.nav.aap.postmottak.klient.joark.JoarkClient
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class SettFagsakSteg(
    private val journalpostRepository: JournalpostRepository,
    private val saksnummerRepository: SaksnummerRepository,
    private val joarkKlient: Joark
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return SettFagsakSteg(
                JournalpostRepositoryImpl(connection),
                SaksnummerRepository(connection),
                JoarkClient()
            )
        }

        override fun type(): StegType {
            return StegType.SETT_FAGSAK
        }
    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)
        val saksvurdering = saksnummerRepository.hentSakVurdering(kontekst.behandlingId) ?: error {"Mangler saksvurdering i journalføringssteg"}

        requireNotNull(journalpost)

        // TODO: Skill mellom maskinell og manuell journalføring
        if (saksvurdering.generellSak){
            joarkKlient.førJournalpostPåGenerellSak(journalpost)
        } else {
            joarkKlient.førJournalpostPåFagsak(
                journalpost, saksvurdering.saksnummer!!
            )
        }

        return Fullført
    }

}