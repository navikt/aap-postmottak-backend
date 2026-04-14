package no.nav.aap.postmottak.prosessering

import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

private const val JOURNALPOST_ID_KEY = "journalpostId"
private const val INNKOMMENDE_JOURNALPOST_ID_KEY = "innkommendeJournalpostId"
private const val SAKSNUMMER_KEY = "saksnummer"

internal fun JobbInput.getJournalpostId() = this.parameter(JOURNALPOST_ID_KEY).toLong().let(::JournalpostId)

fun JobbInput.medJournalpostId(journalpostId: JournalpostId) =
    this.medParameter(JOURNALPOST_ID_KEY, journalpostId.toString())

// Dette er primary key til innkommende_journalpost
internal fun JobbInput.getInnkommendeJournalpostId() = this.parameter(INNKOMMENDE_JOURNALPOST_ID_KEY).toLong()
internal fun JobbInput.medInnkommendeJournalpostId(id: Long) =
    this.medParameter(INNKOMMENDE_JOURNALPOST_ID_KEY, id.toString())

internal fun JobbInput.getSaksnummer() = this.parameter(SAKSNUMMER_KEY)
fun JobbInput.medSaksnummer(saksnummer: String) =
    this.medParameter(SAKSNUMMER_KEY, saksnummer)
