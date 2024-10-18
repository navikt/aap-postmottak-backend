package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.søknad

import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.postmottak.klient.behandlingsflyt.DigitalSøknadStudent
import no.nav.aap.postmottak.klient.behandlingsflyt.ErStudentStatus
import no.nav.aap.postmottak.klient.behandlingsflyt.HarYrkesskadeStatus
import no.nav.aap.postmottak.klient.behandlingsflyt.OppgitteBarn
import no.nav.aap.postmottak.klient.behandlingsflyt.SkalGjenopptaStudieStatus
import no.nav.aap.postmottak.klient.behandlingsflyt.Søknad
import no.nav.aap.postmottak.klient.behandlingsflyt.SøknadStudent
import kotlin.text.Charsets.UTF_8

data class DigitalSøknad(
    val student: DigitalSøknadStudent? = null,
    val yrkesskade: HarYrkesskadeStatus? = null,
    val oppgitteBarn: OppgitteBarn? = null,
)

fun ByteArray.parseDigitalSøknad(): DigitalSøknad {
    val str = String(this, UTF_8)
    return str.parseDigitalSøknad()
}

fun String.parseDigitalSøknad(): DigitalSøknad {
    return DefaultJsonMapper.fromJson(this, DigitalSøknad::class.java)
}

fun DigitalSøknad.serialiser(): String {
    return DefaultJsonMapper.toJson(this)
}

fun DigitalSøknad.berik(): Søknad {
    return Søknad(
        student = SøknadStudent(
            erStudent = this.student?.erStudent ?: ErStudentStatus.IKKE_OPPGITT,
            kommeTilbake = this.student?.kommeTilbake ?: SkalGjenopptaStudieStatus.IKKE_OPPGITT,
        ),
        yrkesskade = this.yrkesskade ?: HarYrkesskadeStatus.IKKE_OPPGITT,
        oppgitteBarn = this.oppgitteBarn
    )
}