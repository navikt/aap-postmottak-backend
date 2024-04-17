package no.nav.aap.behandlingsflyt.faktagrunnlag.register.Institusjonsopphold

import java.time.LocalDate

class Institusjonsopphold(val institusjonstype: Institusjonstype, val kategori: Oppholdstype, val startdato: LocalDate, sluttdato: LocalDate?) {
    companion object {
        fun nyttOpphold(institusjonstype: String, kategori: String, startdato: LocalDate, sluttdato: LocalDate?): Institusjonsopphold {
            return Institusjonsopphold(Institusjonstype.valueOf(institusjonstype), Oppholdstype.valueOf(kategori), startdato, sluttdato)
        }
    }

    enum class Institusjonstype(val beskrivelse: String) {
        AS("Alders- og sykehjem"),
        FO("Fengsel"),
        HS("Helseinstitusjon")
    }

    enum class Oppholdstype(val beskrivelse: String) {
        A("Alders- og sykehjem"),
        D("Dagpasient"),
        F("Ferieopphold"),
        H("Heldøgnpasient"),
        P("Fødsel"),
        R("Opptreningsinstitusjon"),
        S("Soningsfange"),
        V("Varetektsfange")
    }
}