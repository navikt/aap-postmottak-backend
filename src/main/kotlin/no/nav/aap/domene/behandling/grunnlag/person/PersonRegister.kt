package no.nav.aap.domene.behandling.grunnlag.person

import no.nav.aap.domene.typer.Ident

object PersonRegister {

    private val personer = HashMap<Ident, Personinfo>()

    fun innhent(identer: List<Ident>): List<Personinfo> {
        return personer
            .filterKeys { ident -> ident in identer }
            .map { it.value }
    }

    fun konstruer(ident: Ident, personinfo: Personinfo) {
        personer[ident] = personinfo
    }
}
