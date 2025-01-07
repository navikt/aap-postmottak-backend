package no.nav.aap.postmottak.test

import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.motor.Motor
import no.nav.aap.postmottak.prosessering.ProsesseringsJobber

object TestMotor {

    private val motor = Motor(InitTestDatabase.dataSource, 2, jobber = ProsesseringsJobber.alle())

    init {
        motor.start()
    }
}