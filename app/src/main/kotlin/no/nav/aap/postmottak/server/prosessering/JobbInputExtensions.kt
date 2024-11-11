package no.nav.aap.postmottak.server.prosessering

import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.LocalDateTime

private const val JOURNALPOST_ID_KEY = "journalpostId"
const val MOTTATT_TID_KEY = "mottattTid"


internal fun JobbInput.getJournalpostId() = this.parameter(JOURNALPOST_ID_KEY).toLong().let(::JournalpostId)
internal fun JobbInput.getMottattTid() = this.parameter(MOTTATT_TID_KEY).let(LocalDateTime::parse)

internal fun JobbInput.medJournalpostId(journalpostId: JournalpostId) = this.medParameter(JOURNALPOST_ID_KEY, journalpostId.toString())
internal fun JobbInput.medMottattdato(mottattdato: LocalDateTime) = this.medParameter(MOTTATT_TID_KEY, mottattdato.toString())
