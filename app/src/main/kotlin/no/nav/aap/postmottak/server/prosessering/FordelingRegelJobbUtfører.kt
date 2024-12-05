package no.nav.aap.postmottak.server.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.fordeler.FordelerRegelService
import no.nav.aap.postmottak.fordeler.InnkommendeJournalpost
import no.nav.aap.postmottak.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.postmottak.fordeler.InnkommendeJournalpostStatus
import no.nav.aap.postmottak.fordeler.regler.RegelInput
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

class FordelingRegelJobbUtfører(
    private val flytJobbRepository: FlytJobbRepository,
    private val journalpostService: JournalpostService,
    private val regelService: FordelerRegelService,
    private val innkommendeJournalpostRepository: InnkommendeJournalpostRepository
) : JobbUtfører {

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return FordelingRegelJobbUtfører(
                FlytJobbRepository(connection),
                JournalpostService.konstruer(connection),
                FordelerRegelService(),
                InnkommendeJournalpostRepository(connection)
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
            status = InnkommendeJournalpostStatus.EVALUERT,
            regelresultat = res
        )

        innkommendeJournalpostRepository.lagre(innkommendeJournalpost)
        opprettVideresendJobb(journalpostId)
    }

    private fun opprettVideresendJobb(journalpostId: JournalpostId) {
        flytJobbRepository.leggTil(
            JobbInput(FordelingVideresendJobbUtfører)
                .medJournalpostId(journalpostId)
                .medCallId()
        )
    }
}