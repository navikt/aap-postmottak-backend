package no.nav.aap.postmottak.api.drift

import no.nav.aap.fordeler.InnkommendeJournalpostStatus
import no.nav.aap.fordeler.Regelresultat
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status
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