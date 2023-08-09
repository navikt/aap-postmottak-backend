package no.nav.aap.domene.behandling.grunnlag.person

import no.nav.aap.domene.typer.Ident

object PersonRegisterMock {

    private var personer = HashMap<Ident, Personinfo>()

    private val LOCK = Object()

    fun innhent(identer: List<Ident>): Set<Personinfo> {
        synchronized(LOCK) {
            return personer
                .filterKeys { ident -> ident in identer }
                .map { it.value }
                .toSet()
        }
    }

    fun konstruer(ident: Ident, personinfo: Personinfo) {
        synchronized(LOCK) {
            personer[ident] = personinfo
        }
    }
}
