package no.nav.aap.postmottak.test.fakes

import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

object TestJournalposter {
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
    val LEGEERKLÆRING_TRUKKET_SAK = JournalpostId(130)
    val UTENLANDSK_ORGNR = JournalpostId(131)
    val KLAGE_ETTERSENDING = JournalpostId(132)
    val NY_SØKNAD_MED_TRUKKET_SAK = JournalpostId(133)

    /** Digital søknad for person med kant-i-kant sak i Arena -> manuell fordeling (AVKLAR_FORDELING). */
    val DIGITAL_SØKNAD_KANT_I_KANT = JournalpostId(134)

    /** Papirsøknad for person med kant-i-kant sak i Arena -> manuell fordeling (AVKLAR_FORDELING). */
    val PAPIR_SØKNAD_KANT_I_KANT = JournalpostId(135)
}


object TestIdenter {
    val DEFAULT_IDENT = Ident("21345345210")
    val DEFAULT_IDENT_2 = Ident("21345345212")
    val IDENT_UTEN_SAK_I_KELVIN = Ident("00000001111")
    val SKJERMET_IDENT = Ident("00000002222")
    val IDENT_MED_SAK_I_ARENA = Ident("0000000333")
    val IDENT_MED_TRUKKET_SAK_I_KELVIN = Ident("0000000444")
    val IDENT_MED_KANT_I_KANT_SAK = Ident("0000000555")
}
