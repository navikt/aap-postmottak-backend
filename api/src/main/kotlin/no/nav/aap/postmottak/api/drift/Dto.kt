package no.nav.aap.postmottak.api.drift

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.fordeler.InnkommendeJournalpost
import no.nav.aap.fordeler.InnkommendeJournalpostStatus
import no.nav.aap.fordeler.NavEnhet
import no.nav.aap.fordeler.Regelresultat
import no.nav.aap.fordeler.ÅrsakTilStatus
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.tilgang.plugin.kontrakt.AuditlogResolverInput
import no.nav.aap.tilgang.plugin.kontrakt.Personreferanse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Base64
import java.util.UUID
import no.nav.aap.postmottak.kontrakt.behandling.Status as BehandlingStatus

data class JournalpostDriftsinfoDto(
    val innkommendeStatus: InnkommendeJournalpostStatus?,
    val brevkode: String? = null,
    val tema: String? = null,
    val fordelingsresultat: Regelresultat? = null,
    val journalstatus: Journalstatus?,
    val mottattDato: LocalDate?,
    val kanal: KanalFraKodeverk?,
    val saksnummer: String?,
    val behandlinger: List<BehandlingDriftsinfo>,
)

data class BehandlingDriftsinfo(
    val referanse: UUID,
    val type: String,
    val status: BehandlingStatus,
    val aktivtSteg: String,
    val opprettet: LocalDateTime,
    val avklaringsbehov: List<ForenkletAvklaringsbehov>,
) {
    companion object {
        fun fra(behandling: Behandling, avklaringsbehovene: List<ForenkletAvklaringsbehov>) =
            BehandlingDriftsinfo(
                referanse = behandling.referanse.referanse,
                type = behandling.typeBehandling.identifikator(),
                status = behandling.status(),
                aktivtSteg = behandling.aktivtSteg().name,
                opprettet = behandling.opprettetTidspunkt,
                avklaringsbehov = avklaringsbehovene,
            )
    }
}

data class ForenkletAvklaringsbehov(
    val definisjon: Definisjon,
    val status: Status,
    val tidsstempel: LocalDateTime = LocalDateTime.now(),
    val endretAv: String,
    val årsakTilSettPåVent: ÅrsakTilSettPåVent?
)

fun krevDtoErUtenFødselsnummer(dto: Any) {
    if (Regex("""(?<!\w)\d{11}(?!\w)""").containsMatchIn(DefaultJsonMapper.toJson(dto))) {
        throw IkkeTillattException("DTO-en inneholder (potensielt) sensitive personopplysninger!")
    }
}

data class PersonSøkDriftsinfoDto(val journalposter: List<InnkommendeJournalpostDto>)
data class SaksnummerPathParam(@param:PathParam("saksnummer") val saksnummer: String)
data class SakJournalposterDto(val journalposter: List<JournalpostPåSakDto>)

data class JournalpostPåSakDto(
    val journalpostId: JournalpostId,
    val journalstatus: Journalstatus?,
    val mottattDato: LocalDate?,
    val kanal: KanalFraKodeverk?,
    val tema: String?,
)

data class IdentDto(val ident: String) : Personreferanse {
    override fun hentPersonreferanse(): String = ident
}

data class InnkommendeJournalpostDto(
    val journalpostId: JournalpostId,
    val brevkode: String?,
    val behandlingstema: String?,
    val status: InnkommendeJournalpostStatus,
    val regelresultat: Regelresultat? = null,
    val årsakTilStatus: ÅrsakTilStatus? = null,
    val enhet: NavEnhet? = null,
) {
    companion object {
        
        fun fraDomene(innkommendeJournalpost: InnkommendeJournalpost): InnkommendeJournalpostDto {
            return InnkommendeJournalpostDto(
                journalpostId = innkommendeJournalpost.journalpostId,
                brevkode = innkommendeJournalpost.brevkode,
                behandlingstema = innkommendeJournalpost.behandlingstema,
                status = innkommendeJournalpost.status,
                regelresultat = innkommendeJournalpost.regelresultat,
                årsakTilStatus = innkommendeJournalpost.årsakTilStatus,
                enhet = innkommendeJournalpost.enhet,
            )
        }
    }
}

data class EttersendForPersonRequest(
    val ident: String,
    val pdfBase64: String,
    val filNavn: String,
    val eksternReferanseId: String?,
) : Personreferanse, AuditlogResolverInput {
    override fun hentPersonreferanse(): String = ident.trim()
    override fun hentAuditlogResolverInput(): String = ident.trim()
}

data class EttersendForPersonResponse(
    val journalpostId: Long,
)

data class EttersendForPersonInput(
    val ident: String,
    val pdf: ByteArray,
    val filNavn: String,
    val eksternReferanseId: UUID?,
)

fun EttersendForPersonRequest.valider(): EttersendForPersonInput {
    val normalisertIdent = ident.trim()
    if(!normalisertIdent.matches(Regex("^\\d{11}$"))) {
        throw UgyldigForespørselException("Ident må være 11 siffer")
    }

    val eksternRef = if(eksternReferanseId != null) {
        requireUuid(eksternReferanseId.trim(), "eksternReferanseId må være en UUID")
    } else {
        null
    }
    val pdf = requirePdf(pdfBase64)

    return EttersendForPersonInput(
        ident = normalisertIdent,
        pdf = pdf,
        filNavn = filNavn,
        eksternReferanseId = eksternRef,
    )
}

private fun requirePdf(pdfBase64: String): ByteArray {
    val bytes = try {
        Base64.getDecoder().decode(pdfBase64)
    } catch (_: IllegalArgumentException) {
        throw UgyldigForespørselException("pdfBase64 er ikke gyldig base64")
    }
    if(bytes.size > 10 * 1024 * 1024){
        throw UgyldigForespørselException("PDF kan ikke være større enn 10 MB")
    }
    if (!bytes.hasPdfHeader()){
        throw UgyldigForespørselException("Innholdet er ikke en gyldig PDF")
    }
    return bytes
}

private fun ByteArray.hasPdfHeader(): Boolean {
    val header = byteArrayOf('%'.code.toByte(), 'P'.code.toByte(), 'D'.code.toByte(), 'F'.code.toByte(), '-'.code.toByte())
    return size >= header.size && header.indices.all { this[it] == header[it] }
}

private fun requireUuid(value: String, message: String): UUID = try {
    UUID.fromString(value)
} catch (_: IllegalArgumentException) {
    throw UgyldigForespørselException(message)
}
