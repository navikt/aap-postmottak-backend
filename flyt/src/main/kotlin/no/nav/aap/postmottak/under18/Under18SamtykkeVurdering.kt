package no.nav.aap.postmottak.under18

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

data class Under18SamtykkeVurdering(
    val beggeForeldreSamtykket: Boolean,
    val samtykkeJournalpostIder: List<JournalpostId>,
)
