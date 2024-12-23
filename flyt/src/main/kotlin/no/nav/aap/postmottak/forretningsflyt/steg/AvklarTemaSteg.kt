package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.AvklarTemaRepository
import no.nav.aap.postmottak.flyt.steg.Avbrutt
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import org.slf4j.LoggerFactory


private val log = LoggerFactory.getLogger(AvklarTemaSteg::class.java)

class AvklarTemaSteg(
    private val journalpostRepository: JournalpostRepository,
    private val avklarTemaRepository: AvklarTemaRepository,
    private val gosysOppgaveGateway: GosysOppgaveGateway
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            return AvklarTemaSteg(
                repositoryProvider.provide(JournalpostRepository::class),
                repositoryProvider.provide(AvklarTemaRepository::class),
                GatewayProvider.provide(GosysOppgaveGateway::class)
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
            log.info("Journalpost har endret tema. Nytt tema er: ${journalpost.tema}")
            gosysOppgaveGateway.finnOppgaverForJournalpost(journalpost.journalpostId)
                .forEach {gosysOppgaveGateway.ferdigstillOppgave(it) }
            return Avbrutt
        }

        return if (!journalpost.erDigitalSøknad() && avklarTemaRepository.hentTemaAvklaring(kontekst.behandlingId) == null) {
            FantAvklaringsbehov(Definisjon.AVKLAR_TEMA)
        } else Fullført
    }
}