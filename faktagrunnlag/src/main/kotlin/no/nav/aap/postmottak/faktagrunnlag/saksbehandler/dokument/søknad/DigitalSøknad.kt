package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.søknad

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import kotlin.text.Charsets.UTF_8

fun ByteArray.parseDigitalSøknad(): SøknadV0 {
    val str = String(this, UTF_8)
    return str.parseDigitalSøknad()
}

fun String.parseDigitalSøknad(): SøknadV0 {
    return DefaultJsonMapper.fromJson(this, SøknadV0::class.java)
}

fun SøknadV0.serialiser(): String {
    return DefaultJsonMapper.toJson(this)
}