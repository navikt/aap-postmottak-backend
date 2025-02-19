package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import org.slf4j.LoggerFactory

class JournalpostInformasjonskrav(
    private val journalpostRepository: JournalpostRepository,
    private val journalpostService: JournalpostService,
) : Informasjonskrav {
    private val log = LoggerFactory.getLogger(JournalpostInformasjonskrav::class.java)


    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): JournalpostInformasjonskrav {
            val repositoryProvider = RepositoryProvider(connection)
            return JournalpostInformasjonskrav(
                repositoryProvider.provide(JournalpostRepository::class),
                JournalpostService.konstruer(connection)
            )
        }
    }

    override fun oppdater(kontekst: FlytKontekst): Informasjonskrav.Endret {
        val persistertJournalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)

        val journalpost = journalpostService.hentJournalpost(kontekst.journalpostId)

        if (persistertJournalpost != journalpost) {
            log.info("Fant endringer i journalpost")
            journalpostRepository.lagre(journalpost)
            return ENDRET
        }

        return IKKE_ENDRET
    }
}

