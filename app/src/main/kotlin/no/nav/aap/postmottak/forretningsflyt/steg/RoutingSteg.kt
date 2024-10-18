package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.klient.AapInternApiClient
import no.nav.aap.postmottak.klient.joark.Journalpost
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import org.slf4j.LoggerFactory


private val log = LoggerFactory.getLogger(RoutingSteg::class.java)

class RoutingSteg(
    private val journalpostRepository: JournalpostRepository,
    private val aapInternApiClient: AapInternApiClient
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return RoutingSteg(
                JournalpostRepositoryImpl(connection),
                AapInternApiClient()
            )
        }

        override fun type(): StegType {
            return StegType.ROUTING
        }
    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)
        check(journalpost is Journalpost.MedIdent) { "journalpost må ha ident" }

        if (!aapInternApiClient.hentArenaSakerForIdent(journalpost.personident.id).isEmpty()) {
            log.info("Fant saker i arena, avbryter flyt for journalpost ${journalpost.journalpostId}")
            return StegResultat(avbrytFlyt = true)
        }
        return StegResultat()
    }
}