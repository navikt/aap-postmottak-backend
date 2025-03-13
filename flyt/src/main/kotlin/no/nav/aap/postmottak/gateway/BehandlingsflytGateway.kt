package no.nav.aap.postmottak.gateway

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AnnetRelevantDokumentV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.gateway.Gateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.text.Charsets.UTF_8

interface BehandlingsflytGateway : Gateway {
    fun finnEllerOpprettSak(ident: Ident, mottattDato: LocalDate): BehandlingsflytSak
    fun finnSaker(ident: Ident): List<BehandlingsflytSak>
    fun sendHendelse(
        journalpostId: JournalpostId,
        kanal: KanalFraKodeverk,
        mottattDato: LocalDateTime,
        innsendingstype: InnsendingType,
        saksnummer: String,
        melding: Melding?
    )
}

data class BehandlingsflytSak(
    val saksnummer: String,
    val periode: Periode,
)

object DokumentTilMeldingParser {
    fun parseTilMelding(dokument: String?, innsendingType: InnsendingType): Melding? {
        return when (innsendingType) {
            InnsendingType.SØKNAD -> DefaultJsonMapper.fromJson(dokument!!, SøknadV0::class.java)
            InnsendingType.MELDEKORT -> DefaultJsonMapper.fromJson(dokument!!, MeldekortV0::class.java)
            InnsendingType.ANNET_RELEVANT_DOKUMENT -> DefaultJsonMapper.fromJson(
                dokument!!,
                AnnetRelevantDokumentV0::class.java
            )
            else -> null
        }
    }

    fun parseTilMelding(dokument: ByteArray?, innsendingType: InnsendingType): Melding? {
        val str = if (dokument != null) String(dokument, UTF_8) else null
        return parseTilMelding(str, innsendingType)
    }
}

fun Melding.serialiser(): String {
    return DefaultJsonMapper.toJson(this)
}
