package no.nav.aap.postmottak.prosessering

import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.FordelingRegelJobbUtfører
import no.nav.aap.postmottak.prosessering.getJournalpostId
import no.nav.aap.postmottak.prosessering.medJournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JobbInputExtensionsKtTest {

    @Test
    fun `Vi kan sette og hente parametere for jobben`() {
        val journalpostId = JournalpostId(1)
        val input = JobbInput(FordelingRegelJobbUtfører)
            .forSak(journalpostId.referanse)
            .medJournalpostId(journalpostId)

        val actualJournalpostId = input.getJournalpostId()

        assertThat(actualJournalpostId).isEqualTo(actualJournalpostId)
    }

}