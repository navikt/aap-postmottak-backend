package no.nav.aap.postmottak.gateway

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AnnetRelevantDokumentV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandlingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
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

    fun finnKlagebehandlinger(saksnummer: Saksnummer): List<Klagebehandling>
}

/**
 * @see no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.SaksinfoDTO
 */
data class BehandlingsflytSak(
    val saksnummer: String,
    val periode: Periode,
    val resultat: ResultatKode? = null,
    val opprettetNy: Boolean
)

data class Klagebehandling(
    val behandlingsReferanse: UUID,
    val opprettetDato: LocalDate
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

            InnsendingType.NY_ÅRSAK_TIL_BEHANDLING -> DefaultJsonMapper.fromJson(
                dokument!!,
                NyÅrsakTilBehandlingV0::class.java
            )

            InnsendingType.KLAGE -> DefaultJsonMapper.fromJson(dokument!!, KlageV0::class.java)
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
