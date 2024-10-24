import no.nav.aap.postmottak.test.Fakes
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

interface WithFakes {
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