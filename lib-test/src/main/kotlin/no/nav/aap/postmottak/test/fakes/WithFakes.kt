package no.nav.aap.postmottak.test.fakes

import no.nav.aap.postmottak.test.Fakes
import org.junit.jupiter.api.BeforeAll

object FakeSingleton {
    val fakes: Fakes = Fakes()
}

interface WithFakes {
    companion object {
        lateinit var fakes: Fakes

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            fakes = FakeSingleton.fakes
        }

    }
}