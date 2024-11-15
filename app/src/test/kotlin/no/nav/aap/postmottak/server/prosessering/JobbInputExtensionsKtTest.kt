package no.nav.aap.postmottak.server.prosessering

import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JobbInputExtensionsKtTest {

    @Test
    fun `Vi kan sette og hente parametere for jobben`() {
        val journalpostId = JournalpostId(1)
        val mottattId = "ID"
        val input = JobbInput(FordelingRegelJobbUtfører)
            .medJournalpostId(journalpostId)
            .medMeldingId(mottattId)

        val actualJournalpostId = input.getJournalpostId()
        val actualMottaTid = input.getMeldingId()

        assertThat(actualJournalpostId).isEqualTo(actualJournalpostId)
        assertThat(actualMottaTid).isEqualTo(mottattId)
    }

}