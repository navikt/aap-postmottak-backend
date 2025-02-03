package no.nav.aap.postmottak.test.fakes

import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

val DIGITAL_SØKNAD_ID = JournalpostId(999)
val SØKNAD_ETTERSENDELSE = JournalpostId(1000)
val UTEN_AVSENDER_MOTTAKER = JournalpostId(11)
val LEGEERKLÆRING = JournalpostId(120)
val ANNET_TEMA = JournalpostId(121)
val UGYLDIG_STATUS = JournalpostId(122)
val STATUS_JOURNALFØRT = JournalpostId(123)
val PAPIR_SØKNAD = JournalpostId(124)
val LEGEERKLÆRING_IKKE_TIL_KELVIN = JournalpostId(125)

val DEFAULT_IDENT = Ident("21345345210")
val IDENT_UTEN_SAK = Ident("00000001111")
