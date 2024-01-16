package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.verdityper.sakogbehandling.Ident

internal fun ident(): Ident {
    return Ident(hentNesteIdent().toString())
}

private fun hentNesteIdent(): Long {
    return InitTestDatabase.dataSource.transaction { connection ->
        connection.queryFirst("SELECT nextval('TESTIDENT') AS IDENT") {
            setRowMapper { row ->
                row.getLong("IDENT")
            }
        }
    }
}
