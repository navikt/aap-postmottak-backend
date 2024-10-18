package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.søknad

import no.nav.aap.postmottak.klient.behandlingsflyt.ErStudentStatus
import no.nav.aap.postmottak.klient.behandlingsflyt.HarYrkesskadeStatus
import no.nav.aap.postmottak.klient.behandlingsflyt.SkalGjenopptaStudieStatus
import no.nav.aap.postmottak.klient.behandlingsflyt.Søknad
import no.nav.aap.postmottak.klient.behandlingsflyt.SøknadStudent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DigitalSøknadTest {
    @Test
    fun `Skal berike digital søknad med oppgitte felter`() {
        val dokument = """{
            |"student": {"erStudent": "Ja"}
            |}""".trimMargin().toByteArray()

        val beriketSøknad = dokument.parseDigitalSøknad().berik()

        assertEquals(
            Søknad(
                student = SøknadStudent(ErStudentStatus.JA, SkalGjenopptaStudieStatus.IKKE_OPPGITT),
                yrkesskade = HarYrkesskadeStatus.IKKE_OPPGITT,
                oppgitteBarn = null
            ), beriketSøknad
        )
    }
}