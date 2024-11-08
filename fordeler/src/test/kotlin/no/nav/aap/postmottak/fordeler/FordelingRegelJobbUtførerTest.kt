package no.nav.aap.postmottak.fordeler

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.Motor
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.BeforeAll
import java.time.LocalDateTime

object ProsesseringsJobber {

    fun alle(): List<Jobb> {
        // Legger her alle oppgavene som skal utføres i systemet
        return listOf(
            FordelingRegelJobbUtfører
        )
    }
}

class FordelingRegelJobbUtførerTest {

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

        dataSource.transaction { connection ->
            val innkommendeJournalpostRepository = InnkommendeJournalpostRepository(connection)

            val regler = innkommendeJournalpostRepository.hent(journalpostId).regelresultat

            assertThat(regler).isNotNull()
        }
    }
}