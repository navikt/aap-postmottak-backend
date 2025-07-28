package no.nav.aap.postmottak.prosessering

import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.DigitaliseringsvurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.slf4j.LoggerFactory

class OppryddingJobbUtfører(
    private val behandlingRepository: BehandlingRepository,
    private val avklarTemaRepository: AvklarTemaRepository,
    private val digitaliseringsvurderingRepository: DigitaliseringsvurderingRepository,
    private val innkommendeJournalpostRepository: InnkommendeJournalpostRepository,
    private val journalpostGateway: JournalpostGateway,
    private val flytJobbRepository: FlytJobbRepository,
) : JobbUtfører {
    private val log = LoggerFactory.getLogger(OppryddingJobbUtfører::class.java)

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            val repositoryProvider = RepositoryProvider(connection)

            return OppryddingJobbUtfører(
                repositoryProvider.provide(BehandlingRepository::class),
                repositoryProvider.provide(AvklarTemaRepository::class),
                repositoryProvider.provide(DigitaliseringsvurderingRepository::class),
                repositoryProvider.provide(InnkommendeJournalpostRepository::class),
                GatewayProvider.provide(JournalpostGateway::class),
                FlytJobbRepository(connection),
            )
        }

        override fun type() = "opprydding.journalført"

        override fun navn() = "Opprydding journalførte journalpost"

        override fun beskrivelse() = "Rydder i data tilknyttet journalposter"

    }

    override fun utfør(input: JobbInput) {
        val journalpostId = input.getJournalpostId()

        val journalpost = journalpostGateway.hentJournalpost(journalpostId)
        if (journalpost.journalstatus != Journalstatus.JOURNALFOERT) {
            log.error("Kan ikke starte opprydding for journalpost $journalpostId med status ${journalpost.journalstatus}")
            return
        }

        if (innkommendeJournalpostRepository.eksisterer(journalpostId)) {
            behandleAlleredeMottatt(journalpostId)
        } else {
            log.info("Journalpost $journalpostId er journalført utenfor Kelvin")
            opprettNyBehandling(journalpostId)
        }
    }

    private fun behandleAlleredeMottatt(journalpostId: JournalpostId) {
        val behandlinger = behandlingRepository.hentAlleBehandlinger(journalpostId)

        val temaVurdering = behandlinger
            .filter { it.typeBehandling == TypeBehandling.Journalføring }
            .firstNotNullOfOrNull { avklarTemaRepository.hentTemaAvklaring(it.id) }

        if (temaVurdering?.skalTilAap == true) {
            log.info("Tema er allerede vurdert som AAP - avbryter opprydding")
            return
        }

        val digitaliseringsbehandling =
            behandlinger.firstOrNull { it.typeBehandling == TypeBehandling.DokumentHåndtering }

        if (digitaliseringsbehandling == null) {
            log.info("Ingen digitaliseringsbehandling funnet for journalpostId=$journalpostId - oppretter ny")
            opprettNyBehandling(journalpostId)
            return
        }

        log.info("Fant tidligere behandling for digitalisering – sjekker om det finnes vurdering")
        val eksisterendeVurdering = digitaliseringsvurderingRepository.hentHvisEksisterer(digitaliseringsbehandling.id)

        if (eksisterendeVurdering == null) {
            log.info("Fant ingen vurdering for digitalisering av journalpost $journalpostId")
            opprettNyBehandling(journalpostId)
        } else {
            log.info("Journalpost $journalpostId har allerede en vurdering for digitalisering")
        }
    }

    private fun opprettNyBehandling(journalpostId: JournalpostId) {
        log.info("Oppretter behandling for digitalisering av journalpost $journalpostId")

        val behandling = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
        flytJobbRepository.leggTil(
            JobbInput(ProsesserBehandlingJobbUtfører)
                .forBehandling(journalpostId.referanse, behandling.id).medCallId()
        )
    }

}
