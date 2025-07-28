package no.nav.aap.postmottak.mottak

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.postmottak.mottak.JoarkRegel.erIkkeKanalEESSI
import no.nav.aap.postmottak.mottak.JoarkRegel.harStatusJournalført
import no.nav.aap.postmottak.mottak.JoarkRegel.harStatusMottatt
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class JoarkRegelTest {

    @Test
    fun `Sjekk regel harStatusMottatt`() {
        assertTrue(
            harStatusMottatt("id", record(journalpostStatus = "MOTTATT")),
            "Skal gi true når status er MOTTATT"
        )

        assertFalse(
            harStatusMottatt("id", record(journalpostStatus = "FERDIGSTILT")),
            "Skal gi false når status ikke er MOTTATT"
        )
    }

    @Test
    fun `Sjekk regel harStatusJournalført`() {
        assertTrue(
            harStatusJournalført("id", record(journalpostStatus = "JOURNALFOERT")),
            "Skal gi true når status er JOURNALFOERT"
        )

        assertFalse(
            harStatusJournalført("id", record(journalpostStatus = "MOTTATT")),
            "Skal gi false når status ikke er MOTTATT"
        )
    }

    @Test
    fun `Sjekk regel erIkkeKanalEESSI`() {
        assertTrue(
            erIkkeKanalEESSI("id", record(mottaksKanal = "NAV_NO")),
            "Skal gi true når mottakskanal ikke er EESSI"
        )

        assertFalse(
            erIkkeKanalEESSI("id", record(mottaksKanal = "EESSI")),
            "Skal gi false når mottakskanal er EESSI"
        )
    }

    @Test
    fun `Sjekk regel for erTemaEndretFraAAP`() {
        assertTrue(
            JoarkRegel.erTemaEndretFraAAP("id", record(temaGammelt = "AAP", temaNytt = "XYZ")),
            "Tema endret fra AAP til XYZ skal gi true"
        )

        assertFalse(
            JoarkRegel.erTemaEndretFraAAP("id", record(temaGammelt = "AAP", temaNytt = "AAP")),
            "Tema endret fra AAP til AAP skal gi false"
        )

        assertFalse(
            JoarkRegel.erTemaEndretFraAAP("id", record(temaGammelt = "OPP", temaNytt = "AAP")),
            "Tema endret til AAP skal gi false"
        )
    }

    @Test
    fun `Sjekk regel erTemaAAP`() {
        assertTrue(
            JoarkRegel.erTemaAAP("id", record(temaNytt = "AAP")),
            "Skal gi true når tema er AAP"
        )

        assertFalse(
            JoarkRegel.erTemaAAP("id", record(temaNytt = "OPP")),
            "Skal gi false når tema ikke er AAP"
        )
    }

    private fun record(
        temaGammelt: String? = null,
        temaNytt: String? = null,
        mottaksKanal: String? = null,
        journalpostStatus: String? = null,
    ): JournalfoeringHendelseRecord {
        return mockk<JournalfoeringHendelseRecord>(relaxed = true) {
            every { this@mockk.temaGammelt } returns temaGammelt
            every { this@mockk.temaNytt } returns temaNytt
            every { this@mockk.mottaksKanal } returns mottaksKanal
            every { this@mockk.journalpostStatus } returns journalpostStatus
        }
    }
}
