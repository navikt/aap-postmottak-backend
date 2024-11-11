package no.nav.aap.postmottak.server.prosessering

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.Motor
import no.nav.aap.postmottak.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.fakes.WithFakes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.BeforeAll
import java.time.LocalDateTime

class FordelingRegelJobbUtførerTest: WithFakes {

    companion object {
        private val dataSource = InitTestDatabase.dataSource
        private val motor = Motor(dataSource, 2, jobber = ProsesseringsJobber.alle())

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            motor.start()
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            motor.stop()
        }
    }

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
        dataSource.transaction {
            FlytJobbRepository(it).leggTil(
                JobbInput(FordelingRegelJobbUtfører)
                    .medJournalpostId(journalpostId)
                    .medMottattdato(LocalDateTime.now())
            )
        }

        await(5000) {
            dataSource.transaction(readOnly = true) { connection ->
                val innkommendeJournalpostRepository = InnkommendeJournalpostRepository(connection)

                val regler = innkommendeJournalpostRepository.hent(journalpostId).regelresultat

                assertThat(regler).isNotNull()
            }
        }
    }

    private fun <T> await(maxWait: Long = 5000, block: () -> T): T {
        val currentTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - currentTime <= maxWait) {
            try {
                return block()
            } catch (_: Throwable) {
            }
            Thread.sleep(50)
        }
        return block()
    }
}