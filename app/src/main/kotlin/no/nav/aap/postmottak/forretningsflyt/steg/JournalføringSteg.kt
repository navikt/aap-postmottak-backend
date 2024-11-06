package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.flyt.steg.Avbrutt
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.klient.joark.Joark
import no.nav.aap.postmottak.klient.joark.JoarkClient
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class JournalføringSteg(
    private val journalpostRepository: JournalpostRepository,
    private val saksnummerRepository: SaksnummerRepository,
    private val joarkKlient: Joark
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return JournalføringSteg(
                JournalpostRepositoryImpl(connection),
                SaksnummerRepository(connection),
                JoarkClient()
            )
        }

        override fun type(): StegType {
            return StegType.ENDELIG_JOURNALFØRING
        }
    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)

        requireNotNull(journalpost)

        // TODO: Skill mellom maskinell og manuell journalføring
        joarkKlient.ferdigstillJournalpostMaskinelt(journalpost)

        if (saksnummerRepository.hentSakVurdering(kontekst.behandlingId)?.generellSak == true) {
            return Avbrutt
        }

        return Fullført
    }
}