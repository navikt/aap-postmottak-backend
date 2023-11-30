package no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlagkonstruktør
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class InntektService private constructor() : Grunnlag {

    companion object : Grunnlagkonstruktør {
        override fun konstruer(connection: DBConnection): InntektService {
            return InntektService()
        }
    }

    override fun oppdater(kontekst: FlytKontekst): Boolean {
        return false
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): InntektGrunnlag? {
        return null
    }
}
