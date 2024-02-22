package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.time.LocalDate

fun ident(fakes: Fakes): Ident {
    val ident = Ident(hentNesteIdent().toString())
    fakes.leggTil(TestPerson(setOf(ident), Fødselsdato(LocalDate.now().minusYears(26))))
    return ident
}

fun ident(): Ident {
    val ident = Ident(hentNesteIdent().toString())
    return ident
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
