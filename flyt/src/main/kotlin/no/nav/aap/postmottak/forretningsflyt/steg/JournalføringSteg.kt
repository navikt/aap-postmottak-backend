package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.Tema
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.JournalføringsGateway
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.kontrakt.steg.StegType

class JournalføringSteg(
    private val journalpostRepository: JournalpostRepository,
    private val joarkKlient: JournalføringsGateway,
    private val avklarTemaRepository: AvklarTemaRepository
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            return JournalføringSteg(
                repositoryProvider.provide(JournalpostRepository::class),
                GatewayProvider.provide(JournalføringsGateway::class),
                repositoryProvider.provide(AvklarTemaRepository::class)
            )
        }

        override fun type(): StegType {
            return StegType.ENDELIG_JOURNALFØRING
        }
    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)
        requireNotNull(journalpost)

        val temavurdering = avklarTemaRepository.hentTemaAvklaring(kontekst.behandlingId)
            ?: error("Tema skal være avklart før JournalføringSteg")

        if (temavurdering.tema == Tema.UKJENT) {
            // Journalpost er blitt håndtert i Gosys
            return Fullført
        }

        joarkKlient.ferdigstillJournalpostMaskinelt(journalpost.journalpostId)

        return Fullført
    }
}