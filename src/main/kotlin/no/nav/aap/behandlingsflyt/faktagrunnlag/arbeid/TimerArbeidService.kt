package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlagkonstruktør
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class TimerArbeidService private constructor() : Grunnlag {

    companion object : Grunnlagkonstruktør {
        override fun konstruer(connection: DBConnection): TimerArbeidService {
            return TimerArbeidService()
        }
    }

    override fun oppdater(kontekst: FlytKontekst): Boolean {
        return false
    }
}
