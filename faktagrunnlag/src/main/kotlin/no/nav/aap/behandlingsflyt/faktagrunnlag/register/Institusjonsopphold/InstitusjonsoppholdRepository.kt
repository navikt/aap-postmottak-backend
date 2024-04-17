package no.nav.aap.behandlingsflyt.faktagrunnlag.register.Institusjonsopphold

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class InstitusjonsoppholdRepository(private val connection: DBConnection) {
    fun hentHvisEksisterer(behandlingId: BehandlingId): Institusjonsopphold? {
        //fix later
        return null
    }

    fun lagre(behandlingId: BehandlingId, institusjonsopphold: Institusjonsopphold?) {
        //fix later
    }

    fun kopier(fraBehandling:BehandlingId, tilBehandling:BehandlingId){
        //fix later
    }

}