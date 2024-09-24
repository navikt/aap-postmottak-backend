package no.nav.aap.postmottak.faktagrunnlag

import no.nav.aap.komponenter.dbconnect.DBConnection

interface Informasjonskravkonstruktør {
    fun konstruer(connection: DBConnection): Informasjonskrav
}
