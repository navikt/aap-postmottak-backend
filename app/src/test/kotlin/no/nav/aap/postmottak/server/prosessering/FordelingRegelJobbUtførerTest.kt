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
import java.time.LocalDateTime

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
        val mottattTid = LocalDateTime.now()
        val input = JobbInput(FordelingRegelJobbUtfører)
            .medJournalpostId(journalpostId)
            .medMottattdato(mottattTid)

        val actualJournalpostId = input.getJournalpostId()
        val actualMottaTid = input.getMottattTid()

        assertThat(actualJournalpostId).isEqualTo(actualJournalpostId)
        assertThat(actualMottaTid).isEqualTo(mottattTid)
    }

    @Test
    fun `når joben er utført finnes det et regel resultat for journalposten`() {
        val journalpostId = JournalpostId(1L)

        val regelResultat = Regelresultat(mapOf("yolo" to true))

        every { regelService.evaluer(any()) } returns regelResultat

        fordelingRegelJobbUtfører.utfør(JobbInput(FordelingRegelJobbUtfører)
            .medJournalpostId(journalpostId)
            .medMottattdato(LocalDateTime.now()))

        verify { innkommendeJournalpostRepository.lagre(withArg {
            assertThat(it.journalpostId).isEqualTo(journalpostId)
            assertThat(it.regelresultat).isEqualTo(regelResultat)
        }) }

        verify { flytJobbRepository.leggTil(withArg {
            assertThat(it.getJournalpostId()).isEqualTo(journalpostId)
        }) }
    }

}