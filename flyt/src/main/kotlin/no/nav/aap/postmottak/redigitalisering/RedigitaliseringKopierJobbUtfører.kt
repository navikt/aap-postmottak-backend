package no.nav.aap.postmottak.redigitalisering

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.gateway.JournalføringService
import no.nav.aap.postmottak.prosessering.getJournalpostId
import no.nav.aap.postmottak.prosessering.getSaksnummer
import no.nav.aap.postmottak.prosessering.medJournalpostId
import no.nav.aap.postmottak.prosessering.medSaksnummer
import org.slf4j.LoggerFactory
import java.util.UUID

class RedigitaliseringKopierJobbUtfører(
    private val journalpostRepository: JournalpostRepository,
    private val journalføringService: JournalføringService,
    private val flytJobbRepository: FlytJobbRepository,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return RedigitaliseringKopierJobbUtfører(
                journalpostRepository = repositoryProvider.provide(),
                journalføringService = JournalføringService(gatewayProvider),
                flytJobbRepository = repositoryProvider.provide(),
            )
        }

        override val type = "redigitalisering.kopier"
        override val navn = "Kopier journalpost for redigitalisering"
        override val beskrivelse = "Kopierer en journalpost i joark og lagrer den nye journalposten lokalt"
    }

    override fun utfør(input: JobbInput) {
        val kildeJournalpostId = input.getJournalpostId()
        val saksnummer = input.getSaksnummer()

        val eksisterendeJournalpost = requireNotNull(journalpostRepository.hentHvisEksisterer(kildeJournalpostId)) {
            "Journalpost ikke funnet for kildeJournalpostId=$kildeJournalpostId"
        }

        log.info("Kopierer journalpost $kildeJournalpostId i joark for redigitalisering")
        val nyJournalpostId = journalføringService.kopierJournalpost(
            kildeJournalpostId = kildeJournalpostId,
            eksternReferanseId = UUID.randomUUID().toString(),
        )

        journalpostRepository.lagre(eksisterendeJournalpost.copy(journalpostId = nyJournalpostId))

        log.info("Journalpost kopiert. Ny journalpostId=$nyJournalpostId. Legger til behandlingsjobb.")
        flytJobbRepository.leggTil(
            JobbInput(RedigitaliseringBehandlingJobbUtfører)
                .forSak(nyJournalpostId.referanse)
                .medJournalpostId(nyJournalpostId)
                .medSaksnummer(saksnummer)
                .medCallId()
        )
    }
}
