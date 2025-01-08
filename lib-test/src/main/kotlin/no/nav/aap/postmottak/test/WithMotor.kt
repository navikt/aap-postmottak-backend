package no.nav.aap.postmottak.test

import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.motor.Motor
import no.nav.aap.postmottak.prosessering.ProsesseringsJobber
import org.junit.jupiter.api.BeforeAll

interface WithMotor {

    companion object {
        private val motor = Motor(InitTestDatabase.dataSource, 2, jobber = ProsesseringsJobber.alle())

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            if (!motor.kjører()) {
                motor.start()
            }
        }
    }

}