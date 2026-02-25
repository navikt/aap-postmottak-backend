package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.gateway.BrukerIdType
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
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): JournalpostInformasjonskrav {
            return JournalpostInformasjonskrav(
                repositoryProvider.provide(JournalpostRepository::class),
                JournalpostService.konstruer(repositoryProvider, gatewayProvider),
                repositoryProvider.provide(SaksnummerRepository::class),
                repositoryProvider.provide(AvklarTemaRepository::class)
            )
        }
    }

    override fun oppdater(kontekst: FlytKontekst): Informasjonskrav.Endret {
        val persistertJournalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)

        val safJournalpost = journalpostService.hentSafJournalpost(kontekst.journalpostId)

        if (safJournalpost.bruker?.type == BrukerIdType.ORGNR) {
            /*
            * Kelvin har ikke støtte for å behandle journalposter tilknyttet organisasjonsnummer.
            * Hvis journalposten er journalført er det ikke lenger relevant å behandle den i Kelvin
            */
            require(safJournalpost.journalstatus == Journalstatus.JOURNALFOERT) {
                "Journalpost ${safJournalpost.journalpostId} med orgnr som bruker må være journalført. " +
                        "Har status: ${safJournalpost.journalstatus}"
            }

            log.info("Journalpost ${safJournalpost.journalpostId} har orgnr som bruker og er journalført. " +
                    "Lagrer journalposten uten å sjekke for relevante endringer.")

            // Lagre oppdatert journalpost med forrige person for å unngå følgefeil i oppgave
            val oppdatertJournalpost = safJournalpost.tilJournalpost(persistertJournalpost?.person!!)
            journalpostRepository.lagre(oppdatertJournalpost)

            return ENDRET
        }

        val journalpost = journalpostService.tilJournalpostMedDokumentTitler(safJournalpost)

        if (persistertJournalpost != journalpost) {
            journalpostRepository.lagre(journalpost)
            val erEndringerRelevante = erEndringerRelevante(kontekst, journalpost)
            if (erEndringerRelevante) {
                log.info("Fant relevante endringer i journalpost med ID ${journalpost.journalpostId}.")
            }

            return if (erEndringerRelevante) ENDRET else IKKE_ENDRET
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

