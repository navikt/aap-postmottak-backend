package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklarteam.AvklarTemaRepository
import no.nav.aap.postmottak.flyt.steg.Avbrutt
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.klient.gosysoppgave.GosysOppgaveKlient
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import org.slf4j.LoggerFactory


private val log = LoggerFactory.getLogger(AvklarTemaSteg::class.java)

class AvklarTemaSteg(
    private val journalpostRepository: JournalpostRepository,
    private val avklarTemaRepository: AvklarTemaRepository,
    private val gosysOppgaveKlient: GosysOppgaveKlient
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return AvklarTemaSteg(
                JournalpostRepositoryImpl(connection),
                AvklarTemaRepository(connection),
                GosysOppgaveKlient()
            )
        }

        override fun type(): StegType {
            return StegType.AVKLAR_TEMA
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?: error("Journalpost mangler i AvklarTemaSteg")
        if (journalpost.tema != "AAP") {
            log.info("Journalpost har endret tema. ytt tema er: ${journalpost.tema}")
            gosysOppgaveKlient.finnOppgaverForJournalpost(journalpost.journalpostId)
                .forEach {gosysOppgaveKlient.ferdigstillOppgave(it) }
            return Avbrutt
        }

        return if (!journalpost.kanBehandlesAutomatisk() && avklarTemaRepository.hentTemaAvklaring(kontekst.behandlingId) == null) {
            FantAvklaringsbehov(Definisjon.AVKLAR_TEMA)
        } else Fullført
    }
}