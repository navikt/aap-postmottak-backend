package no.nav.aap.domene.sak

import no.nav.aap.domene.person.Person
import no.nav.aap.domene.typer.Periode
import no.nav.aap.domene.typer.Saksnummer
import java.util.Locale

class Sak(val id: Long, val person: Person, val rettighetsperiode: Periode, private var status: Status = Status.OPPRETTET) {

    val saksnummer = Saksnummer(id.toString(36)
        .uppercase(Locale.getDefault())
        .replace("O", "o")
        .replace("I", "i"))

    override fun toString(): String {
        return "Sak(id=$id, person=$person, periode=$rettighetsperiode, status=$status, saksnummer='$saksnummer')"
    }

}
