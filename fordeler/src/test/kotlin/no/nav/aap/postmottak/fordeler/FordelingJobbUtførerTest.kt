package no.nav.aap.postmottak.fordeler

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.Motor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll

object ProsesseringsJobber {

    fun alle(): List<Jobb> {
        // Legger her alle oppgavene som skal utføres i systemet
        return listOf(
            FordelingJobbUtfører
        )
    }
}

class FordelingJobbUtførerTest {

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
    fun utfør() {
        dataSource.transaction {
            FlytJobbRepository(it).leggTil(
                JobbInput(FordelingJobbUtfører)
                    .medParameter("journalpostId", "213452345345")
                    .medCallId()
            )
        }
        Thread.sleep(1000)
    }
}