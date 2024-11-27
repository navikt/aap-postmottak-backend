package no.nav.aap.postmottak.server.prosessering

import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

private const val JOURNALPOST_ID_KEY = "journalpostId"

internal fun JobbInput.getJournalpostId() = this.parameter(JOURNALPOST_ID_KEY).toLong().let(::JournalpostId)

internal fun JobbInput.medJournalpostId(journalpostId: JournalpostId) = this.medParameter(JOURNALPOST_ID_KEY, journalpostId.toString())
