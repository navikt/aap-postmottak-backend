package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.sak.Ident
import java.util.concurrent.atomic.AtomicInteger

private val identTeller = AtomicInteger(0)

fun ident(): Ident {
    return Ident(identTeller.getAndAdd(1).toString())
}
