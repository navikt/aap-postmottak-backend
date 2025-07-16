package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import org.slf4j.LoggerFactory

class JournalpostInformasjonskrav(
    private val journalpostRepository: JournalpostRepository,
    private val journalpostService: JournalpostService,
    private val saksnummerRepository: SaksnummerRepository,
    private val avklarTemaRepository: AvklarTemaRepository
) : Informasjonskrav {
    private val log = LoggerFactory.getLogger(JournalpostInformasjonskrav::class.java)


    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): JournalpostInformasjonskrav {
            val repositoryProvider = RepositoryProvider(connection)
            return JournalpostInformasjonskrav(
                repositoryProvider.provide(JournalpostRepository::class),
                JournalpostService.konstruer(connection),
                repositoryProvider.provide(SaksnummerRepository::class),
                repositoryProvider.provide(AvklarTemaRepository::class)
            )
        }
    }

    override fun oppdater(kontekst: FlytKontekst): Informasjonskrav.Endret {
        val persistertJournalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)

        val journalpost = journalpostService.hentJournalpost(kontekst.journalpostId)

        if (persistertJournalpost != journalpost) {
            log.info("Fant endringer i journalpost")
            journalpostRepository.lagre(journalpost)
            return if (erEndringerRelevante(kontekst, journalpost)) ENDRET else IKKE_ENDRET
        }

        return IKKE_ENDRET
    }

    private fun erEndringerRelevante(kontekst: FlytKontekst, journalpost: Journalpost): Boolean {
        if (journalpost.status == Journalstatus.JOURNALFOERT || journalpost.status == Journalstatus.UTGAAR) {
            return false
        }

        val saksnummerVurdering = saksnummerRepository.hentSakVurdering(kontekst.behandlingId)
        val temaVurdering = avklarTemaRepository.hentTemaAvklaring(kontekst.behandlingId)

        return saksnummerVurdering?.saksnummer != journalpost.saksnummer 
                || temaVurdering?.tema != Tema.fraString(journalpost.tema)
    }
    
}

