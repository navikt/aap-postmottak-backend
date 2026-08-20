package no.nav.aap.postmottak

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.fordeler.NavEnhet
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord

class PrometheusProvider {
    companion object {
        lateinit var prometheus: PrometheusMeterRegistry
    }
}

fun MeterRegistry.fordelingsCounter(system: Fagsystem, erSøknad: Boolean): Counter =
    this.counter(
        "fordeling_videresend", listOf(
            Tag.of("system", system.name),
            Tag.of("erSoknad", erSøknad.toString())
        )
    )

fun MeterRegistry.hendelseType(record: JournalfoeringHendelseRecord): Counter =
    this.counter(
        "joark_hendelse", listOf(
            Tag.of("hendelseType", record.hendelsesType),
        )
    )

fun MeterRegistry.journalpostCounter(brevkode: String?, filtype: String?) =
    this.counter(
        "journalpost",
        listOf(
            Tag.of("brevkode", brevkode?.let { Brevkoder.fraKode(it).name } ?: Brevkoder.ANNEN.name)
        )
    )

fun MeterRegistry.journalføringCounter(type: JournalføringsType, enhet: NavEnhet? = null): Counter {
    val tags = mutableListOf(Tag.of("type", type.name))
    if (enhet != null) {
        tags += Tag.of("enhet", enhet.tilMonitoreringEnhetsgruppe())
    }
    return this.counter(
        "arena_journalfoering", tags
    )
}

fun MeterRegistry.ubehandledeJournalposterCounter(kildesystem: String): Counter =
    this.counter("postmottak_journalposter_ubehandlet", listOf(Tag.of("kildesystem", kildesystem)))

fun MeterRegistry.regelresultat(tilKelvin: Boolean, regel: String): Counter =
    this.counter("postmottak_regelresultat", listOf(Tag.of("regel", regel), Tag.of("til_kelvin", tilKelvin.toString())))

fun MeterRegistry.retriesExceeded(jobbType: String): Counter =
    this.counter("postmottak_retries_exceeded", listOf(Tag.of("jobb_type", jobbType)))

fun MeterRegistry.fordelingAvSoknadVedArenaHistorikkCounter(fagsystem: Fagsystem): Counter = this.counter(
    "postmottak_fordeling_av_soknad_ved_arenahistorikk", listOf(Tag.of("fagsystem", fagsystem.name))
)

fun MeterRegistry.begrensetInntakTilKelvin(sperretAvFilter: Boolean): Counter = this.counter(
    "postmottak_begrensetInntakTilKelvin", listOf(Tag.of("sperret", sperretAvFilter.toString()))
)

fun MeterRegistry.resultatAvSignifikantArenaHistorikkFilterTeller(harSignifikantHistorikk: Boolean): Counter =
    this.counter(
        "postmottak_arenaperson_har_signifikant_historikk",
        listOf(Tag.of("signifikant", harSignifikantHistorikk.toString()))
    )

fun MeterRegistry.søknadOmAapTeller(
    harArenaHistorikk: Boolean,
    harSignifikantArenaHistorikk: Boolean,
    erSøknad: Boolean
): Counter =
    // Mål hvor mange som har arena-historikk i det hele tatt, blant de med søknader
    // Mål hvor mange som har signifikant arena-historikk, blant de med søknader
    this.counter(
        "postmottak_soknad_mottatt",
        listOf(
            Tag.of("arena_historikk", harArenaHistorikk.toString()),
            Tag.of("signifikant_arena_historikk", harSignifikantArenaHistorikk.toString()),
            Tag.of("er_soknad", erSøknad.toString())
        )
    )

fun MeterRegistry.tellAntallTilManuellFordeling(manuellFordeles: Boolean, erSøknad: Boolean) = this.counter(
    "postmottak_soknad_fordeles_manuelt",
    listOf(Tag.of("manuelt", manuellFordeles.toString()), Tag.of("er_soknad", erSøknad.toString()))
)

fun MeterRegistry.tellAntallMaksUtvidetKvoteSnartOppbrukt(maksKvoteSnartOppbrukt: Boolean, erSøknad: Boolean) = this.counter(
    "postmottak_soknad_med_maks_utvidet_kvote",
    listOf(Tag.of("maksKvoteSnartOppbrukt", maksKvoteSnartOppbrukt.toString()), Tag.of("er_soknad", erSøknad.toString()))
)

enum class Fagsystem {
    arena,
    kelvin
}

enum class JournalføringsType {
    automatisk,
    jfr,
    fdr
}

private fun NavEnhet.tilMonitoreringEnhetsgruppe(): String {
    return when {
        this == "0393" -> "Oppfølging utland"
        this == "4491" -> "NAY"
        this == "4402" -> "NAY utland"
        this == "4260" -> "Klageinstans AAP"
        this.startsWith("02") -> "Øst-Viken"
        this.startsWith("03") -> "Oslo"
        this.startsWith("04") -> "Innlandet"
        this.startsWith("06") -> "Vest-Viken"
        this.startsWith("08") -> "Vestfold og Telemark"
        this.startsWith("10") -> "Agder"
        this.startsWith("11") -> "Rogaland"
        this.startsWith("12") -> "Vestland"
        this.startsWith("15") -> "Møre og Romsdal"
        this.startsWith("18") -> "Nordland"
        this.startsWith("19") -> "Troms og Finnmark"
        this.startsWith("57") -> "Trøndelag"
        else -> "Annet"
    }
}