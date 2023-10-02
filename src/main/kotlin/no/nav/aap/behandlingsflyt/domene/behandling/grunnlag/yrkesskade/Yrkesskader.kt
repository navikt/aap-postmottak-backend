package no.nav.aap.behandlingsflyt.domene.behandling.grunnlag.yrkesskade

class Yrkesskader(val yrkesskader: List<Yrkesskade>) {

    fun harYrkesskade(): Boolean {
        return yrkesskader.isNotEmpty()
    }
}
