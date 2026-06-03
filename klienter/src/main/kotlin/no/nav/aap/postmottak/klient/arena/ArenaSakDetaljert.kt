package no.nav.aap.postmottak.klient.arena

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * DTO-er som speiler responsen fra aap-arenaoppslag sitt endepunkt
 * `GET /sak/{sakid}/detaljert` (`ArenaSakDetaljertRespons`).
 *
 * Disse ligger ikke i `aap-arenaoppslag` sin `kontrakt`-modul, og defineres derfor
 * lokalt her i klienten.
 */
data class ArenaSakDetaljertRespons(
    val sakId: String,
    val opprettetAar: Int,
    val lopenr: Int,
    val person: ArenaSakPerson,
    val statuskode: String,
    val statusnavn: String,
    val registrertDato: LocalDateTime,
    val avsluttetDato: LocalDateTime?,
    val vedtak: List<ArenaVedtakMedDetaljer>,
    val telleverkForPerson: TelleverkForPerson?,
    val kvoteHistorikk: Set<KvotebrukHendelse>,
)

data class ArenaSakPerson(
    val personId: Int,
    val fodselsnummer: String,
    val fornavn: String,
    val etternavn: String,
)

data class ArenaVedtakMedDetaljer(
    val vedtakId: Int,
    val lopenrvedtak: Int,
    val statusKode: String,
    val statusNavn: String,
    val vedtaktypeKode: String,
    val vedtaktypeNavn: String,
    val aktivitetsfaseKode: String,
    val aktivitetsfaseNavn: String,
    val fraOgMed: LocalDate?,
    val tilDato: LocalDate?,
    val rettighetkode: String,
    val rettighetnavn: String,
    val utfallkode: String?,
    val begrunnelse: String?,
    val saksbehandler: String?,
    val beslutter: String?,
    val relatertVedtak: Int?,
    val fakta: List<ArenaVedtakfakta> = emptyList(),
    val vilkårsvurderinger: List<ArenaVilkårsvurdering> = emptyList(),
)

data class ArenaVedtakfakta(
    val kode: String,
    val navn: String,
    val verdi: String?,
    val registrertDato: LocalDate,
)

data class ArenaVilkårsvurdering(
    val vilkårsvurderingId: Long,
    val vilkårkode: String,
    val begrunnelse: String?,
    val vurdertAv: String?,
    val vilkårnavn: String,
    val erObligatorisk: Boolean,
    val hjelpetekstUrl: String?,
    val lovtekstUrl: String?,
    val rundskrivUrl: String?,
    val statuskode: String,
    val statusnavn: String,
)

data class TelleverkForPerson(
    val ordineerAAPKvote: Int,
    val utvidetAAPKvote: Int?,
)

data class KvotebrukHendelse(
    val id: Int,
    val kvoteTypeKode: String,
    val endringsGrunnlag: String,
    val antallBevegelse: Int,
    val posteringTypeKode: String,
    val datoHendelse: LocalDate,
    val resterende: Int,
    val modUser: String?,
    val begrunnelse: String?,
)

