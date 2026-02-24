package no.nav.aap.postmottak

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.fordeler.NavEnhet
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord

class PrometheusProvider {
    companion object {
        lateinit var prometheus: PrometheusMeterRegistry
    }
}

fun MeterRegistry.fordelingsCounter(system: Fagsystem): Counter =
    this.counter("fordeling_videresend", listOf(Tag.of("system", system.name)))

fun MeterRegistry.hendelseType(record: JournalfoeringHendelseRecord): Counter =
    this.counter(
        "joark_hendelse", listOf(
            Tag.of("hendelseType", record.hendelsesType),
            Tag.of("status", record.journalpostStatus),
            Tag.of("temaNytt", record.temaNytt),
        )
    )

fun MeterRegistry.journalpostCounter(brevkode: String?, filtype: String?) =
    this.counter(
        "journalpost",
        listOf(
            Tag.of("brevkode", brevkode ?: "Ukjent"),
            Tag.of("filtype", filtype ?: "UKJENT")
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

fun MeterRegistry.personFinnesIArena(medSignifikantHistorikk: Boolean): Counter =
    this.counter(
        "postmottak_person_finnes_i_aap_arena",
        listOf(Tag.of("signifikant_historikk", medSignifikantHistorikk.toString()))
    )

fun MeterRegistry.regelresultat(tilKelvin: Boolean, regel: String): Counter =
    this.counter("postmottak_regelresultat", listOf(Tag.of("regel", regel), Tag.of("til_kelvin", tilKelvin.toString())))

fun MeterRegistry.retriesExceeded(jobbType: String): Counter =
    this.counter("postmottak_retries_exceeded", listOf(Tag.of("jobb_type", jobbType)))

fun MeterRegistry.fordelingVedArenaHistorikkCounter(fagsystem: Fagsystem) =
    this.counter("postmottak_fordeling_ved_arenahistorikk", listOf(Tag.of("fagsystem", fagsystem.name)))

fun MeterRegistry.sperretAvArenaHistorikkFilterTeller(sperret: Boolean) =
    this.counter("postmottak_sperret_av_arenahistorikk_filter", listOf(Tag.of("sperret", sperret.toString())))


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