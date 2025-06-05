package no.nav.aap.postmottak.prosessering

import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

private const val JOURNALPOST_ID_KEY = "journalpostId"
private const val INNKOMMENDE_JOURNALPOST_ID_KEY = "innkommendeJournalpostId"

internal fun JobbInput.getJournalpostId() = this.parameter(JOURNALPOST_ID_KEY).toLong().let(::JournalpostId)

internal fun JobbInput.medJournalpostId(journalpostId: JournalpostId) = this.medParameter(JOURNALPOST_ID_KEY, journalpostId.toString())

// Dette er primary key til innkommende_journalpost
internal fun JobbInput.getInnkommendeJournalpostId() = this.parameter(INNKOMMENDE_JOURNALPOST_ID_KEY).toLong()
internal fun JobbInput.medInnkommendeJournalpostId(id: Long) = this.medParameter(INNKOMMENDE_JOURNALPOST_ID_KEY, id.toString())
