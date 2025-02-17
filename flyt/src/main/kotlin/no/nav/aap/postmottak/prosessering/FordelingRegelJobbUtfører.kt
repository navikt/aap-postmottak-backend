package no.nav.aap.postmottak.prosessering

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.fordeler.FordelerRegelService
import no.nav.aap.fordeler.InnkommendeJournalpost
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.InnkommendeJournalpostStatus
import no.nav.aap.fordeler.regler.RegelInput
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.journalpostCounter
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

class FordelingRegelJobbUtfører(
    private val flytJobbRepository: FlytJobbRepository,
    private val journalpostService: JournalpostService,
    private val regelService: FordelerRegelService,
    private val innkommendeJournalpostRepository: InnkommendeJournalpostRepository,
    private val prometheus: MeterRegistry = SimpleMeterRegistry()
) : JobbUtfører {

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            val repositoryProvider = RepositoryProvider(connection)
            return FordelingRegelJobbUtfører(
                FlytJobbRepository(connection),
                JournalpostService.konstruer(connection),
                FordelerRegelService(connection),
                repositoryProvider.provide(InnkommendeJournalpostRepository::class),
                PrometheusProvider.prometheus
            )
        }

        override fun type() = "fordel.innkommende"

        override fun navn() = "Prosesser fordeling"

        override fun beskrivelse() = "Vurderer mottaker av innkommende journalpost"

    }

    override fun utfør(input: JobbInput) {
        val journalpostId = input.getJournalpostId()

        val journalpost = journalpostService.hentjournalpost(journalpostId)

        val res = regelService.evaluer(
            RegelInput(
                journalpostId.referanse,
                journalpost.person,
                journalpost.hoveddokumentbrevkode
            )
        )

        val innkommendeJournalpost = InnkommendeJournalpost(
            journalpostId = journalpostId,
            brevkode = journalpost.hoveddokumentbrevkode,
            behandlingstema = journalpost.behandlingstema,
            status = InnkommendeJournalpostStatus.EVALUERT,
            regelresultat = res
        )

        innkommendeJournalpostRepository.lagre(innkommendeJournalpost)
        opprettVideresendJobb(journalpostId)

        prometheus.journalpostCounter(journalpost).increment()
    }

    private fun opprettVideresendJobb(journalpostId: JournalpostId) {
        flytJobbRepository.leggTil(
            JobbInput(FordelingVideresendJobbUtfører)
                .forSak(journalpostId.referanse)
                .medJournalpostId(journalpostId)
                .medCallId()
        )
    }
}