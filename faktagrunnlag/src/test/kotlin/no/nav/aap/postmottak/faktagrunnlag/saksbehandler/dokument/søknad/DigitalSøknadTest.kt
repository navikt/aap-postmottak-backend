package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.søknad

import no.nav.aap.postmottak.faktagrunnlag.register.behandlingsflyt.ErStudentStatus
import no.nav.aap.postmottak.faktagrunnlag.register.behandlingsflyt.HarYrkesskadeStatus
import no.nav.aap.postmottak.faktagrunnlag.register.behandlingsflyt.SkalGjenopptaStudieStatus
import no.nav.aap.postmottak.faktagrunnlag.register.behandlingsflyt.Søknad
import no.nav.aap.postmottak.faktagrunnlag.register.behandlingsflyt.SøknadStudent
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