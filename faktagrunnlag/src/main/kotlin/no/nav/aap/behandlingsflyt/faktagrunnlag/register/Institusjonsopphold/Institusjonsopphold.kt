package no.nav.aap.behandlingsflyt.faktagrunnlag.register.Institusjonsopphold

import java.time.LocalDate

class Institusjonsopphold(val institusjonstype: Institusjonstype, val kategori: Oppholdstype, val startdato: LocalDate, val sluttdato: LocalDate?, val orgnr: String? = null) {
    companion object {
        fun nyttOpphold(institusjonstype: String, kategori: String, startdato: LocalDate, sluttdato: LocalDate?, orgnr: String?): Institusjonsopphold {
            return Institusjonsopphold(Institusjonstype.valueOf(institusjonstype), Oppholdstype.valueOf(kategori), startdato, sluttdato, orgnr)
        }
    }

}