package no.nav.aap.postmottak.server.prosessering

import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

private const val JOURNALPOST_ID_KEY = "journalpostId"
const val MELDING_ID_KEY = "mottattTid"


internal fun JobbInput.getJournalpostId() = this.parameter(JOURNALPOST_ID_KEY).toLong().let(::JournalpostId)
internal fun JobbInput.getMeldingId() = this.parameter(MELDING_ID_KEY)

internal fun JobbInput.medJournalpostId(journalpostId: JournalpostId) = this.medParameter(JOURNALPOST_ID_KEY, journalpostId.toString())
internal fun JobbInput.medMeldingId(meldingId: String) = this.medParameter(MELDING_ID_KEY, meldingId)
