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
val STATUS_JOURNALFØRT_ANNET_FAGSYSTEM = JournalpostId(126)
val PERSON_UTEN_SAK_I_BEHANDLINGSFLYT = JournalpostId(127)
val MED_GOSYS_OPPGAVER = JournalpostId(128)
val PERSON_MED_SAK_I_ARENA = JournalpostId(129)

val DEFAULT_IDENT = Ident("21345345210")
val IDENT_UTEN_SAK_I_KELVIN = Ident("00000001111")
val SKJERMET_IDENT = Ident("00000002222")
val IDENT_MED_SAK_I_ARENA = Ident("0000000333")
