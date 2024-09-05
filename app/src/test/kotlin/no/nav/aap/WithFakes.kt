package no.nav.aap

import no.nav.aap.behandlingsflyt.test.Fakes
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

abstract class WithFakes {
    companion object {
        lateinit var fakes: Fakes

        @JvmStatic
        @BeforeAll
        fun beforeAll(): Unit {
            fakes = Fakes()
        }

        @JvmStatic
        @AfterAll
        fun afterAll(): Unit {
            fakes.close()
        }
    }
}