package no.nav.aap.postmottak.fordeler

import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class JobbInputExtensionsKtTest {

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

}