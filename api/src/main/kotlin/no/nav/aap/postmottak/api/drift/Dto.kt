package no.nav.aap.postmottak.api.drift

import no.nav.aap.fordeler.InnkommendeJournalpost
import no.nav.aap.fordeler.InnkommendeJournalpostStatus
import no.nav.aap.fordeler.NavEnhet
import no.nav.aap.fordeler.Regelresultat
import no.nav.aap.fordeler.ÅrsakTilStatus
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.tilgang.plugin.kontrakt.Personreferanse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
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
