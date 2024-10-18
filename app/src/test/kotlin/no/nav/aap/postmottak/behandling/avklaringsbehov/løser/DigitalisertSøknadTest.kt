package no.nav.aap.postmottak.behandling.avklaringsbehov.løser

import no.nav.aap.postmottak.klient.behandlingsflyt.ErStudentStatus
import no.nav.aap.postmottak.klient.behandlingsflyt.HarYrkesskadeStatus
import no.nav.aap.postmottak.klient.behandlingsflyt.SkalGjenopptaStudieStatus
import no.nav.aap.postmottak.klient.behandlingsflyt.Søknad
import no.nav.aap.postmottak.klient.behandlingsflyt.SøknadStudent
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.søknad.berik
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.søknad.parseDigitalSøknad
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DigitalisertSøknadTest {
    @Test
    fun `Skal berike manuell søknad med ikke-oppgitte felter`() {
        val dokument = """{
            |"student": {"erStudent": "Ja"}
            |}""".trimMargin()
        
        val beriketSøknad = dokument.parseDigitalSøknad().berik()
        
        assertEquals(
            Søknad(
            student = SøknadStudent(ErStudentStatus.JA, SkalGjenopptaStudieStatus.IKKE_OPPGITT),
            yrkesskade = HarYrkesskadeStatus.IKKE_OPPGITT,
            oppgitteBarn = null
        ), beriketSøknad)
    }
}