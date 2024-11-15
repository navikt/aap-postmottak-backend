package no.nav.aap.postmottak.server.prosessering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.fordeler.FordelerRegelService
import no.nav.aap.postmottak.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.postmottak.fordeler.Regelresultat
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FordelingRegelJobbUtførerTest {

    val flytJobbRepository: FlytJobbRepository = mockk(relaxed = true)
    val innkommendeJournalpostRepository = mockk<InnkommendeJournalpostRepository>(relaxed = true)
    val regelService = mockk<FordelerRegelService>(relaxed = true)

    val fordelingRegelJobbUtfører = FordelingRegelJobbUtfører(
        flytJobbRepository,
        journalpostService = mockk(relaxed = true),
        regelService = regelService,
        innkommendeJournalpostRepository = innkommendeJournalpostRepository
    )

    @Test
    fun `Vi kan sette og hente parametere for jobben`() {
        val journalpostId = JournalpostId(1)
        val meldingId = "key"
        val input = JobbInput(FordelingRegelJobbUtfører)
            .medJournalpostId(journalpostId)
            .medMeldingId(meldingId)

        val actualJournalpostId = input.getJournalpostId()
        val actualMottaTid = input.getMeldingId()

        assertThat(actualJournalpostId).isEqualTo(actualJournalpostId)
        assertThat(actualMottaTid).isEqualTo(meldingId)
    }

    @Test
    fun `når joben er utført finnes det et regel resultat for journalposten`() {
        val journalpostId = JournalpostId(1L)

        val regelResultat = Regelresultat(mapOf("yolo" to true))

        every { regelService.evaluer(any()) } returns regelResultat

        fordelingRegelJobbUtfører.utfør(JobbInput(FordelingRegelJobbUtfører)
            .medJournalpostId(journalpostId)
            .medMeldingId("key")
        )

        verify { innkommendeJournalpostRepository.lagre(withArg {
            assertThat(it.journalpostId).isEqualTo(journalpostId)
            assertThat(it.regelresultat).isEqualTo(regelResultat)
        }) }

        verify { flytJobbRepository.leggTil(withArg {
            assertThat(it.getJournalpostId()).isEqualTo(journalpostId)
            })
        }
    }

}