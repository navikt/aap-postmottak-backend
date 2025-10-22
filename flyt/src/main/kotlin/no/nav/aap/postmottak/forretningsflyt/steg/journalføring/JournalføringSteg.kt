package no.nav.aap.postmottak.forretningsflyt.steg.journalføring

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.JournalføringService
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.steg.StegType

class JournalføringSteg(
    private val journalpostRepository: JournalpostRepository,
    private val joarkKlient: JournalføringService,
    private val avklarTemaRepository: AvklarTemaRepository
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return JournalføringSteg(
                repositoryProvider.provide(),
                JournalføringService(gatewayProvider),
                repositoryProvider.provide()
            )
        }

        override fun type(): StegType {
            return StegType.ENDELIG_JOURNALFØRING
        }
    }

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val journalpost = requireNotNull(journalpostRepository.hentHvisEksisterer(kontekst.behandlingId))

        if (journalpost.erUgyldig() || journalpost.status == Journalstatus.JOURNALFOERT) return Fullført

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