package no.nav.aap.flyt.steg

import no.nav.aap.domene.behandling.grunnlag.person.PersonRegister
import no.nav.aap.domene.behandling.grunnlag.person.PersoninformasjonTjeneste
import no.nav.aap.domene.behandling.grunnlag.yrkesskade.Yrkesskade
import no.nav.aap.domene.behandling.grunnlag.yrkesskade.YrkesskadeRegister
import no.nav.aap.domene.behandling.grunnlag.yrkesskade.YrkesskadeTjeneste
import no.nav.aap.domene.behandling.grunnlag.yrkesskade.Yrkesskader
import no.nav.aap.domene.person.PersonTjeneste
import no.nav.aap.domene.sak.SakTjeneste
import no.nav.aap.flyt.StegType

class InnhentRegisterdataSteg : BehandlingSteg {

    override fun utfør(input: StegInput): StegResultat {
        val sak = SakTjeneste.hent(input.kontekst.sakId)
        val person = PersonTjeneste.hent(sak.person.identifikator)

        val yrkesskadePeriode = YrkesskadeRegister.innhent(person.identer(), sak.rettighetsperiode)

        val behandlingId = input.kontekst.behandlingId
        if (yrkesskadePeriode.isNotEmpty()) {
            YrkesskadeTjeneste.lagre(
                behandlingId,
                Yrkesskader(yrkesskadePeriode.map { periode -> Yrkesskade("ASDF", periode) })
            )
        } else if (YrkesskadeTjeneste.hentHvisEksisterer(behandlingId).isPresent) {
            YrkesskadeTjeneste.lagre(behandlingId, null)
        }

        val personopplysninger = PersonRegister.innhent(person.identer())
        if (personopplysninger.size != 1) {
            throw IllegalStateException("fant flere personer enn forventet")
        }

        PersoninformasjonTjeneste.lagre(behandlingId, personopplysninger.first())

        return StegResultat() // DO NOTHING
    }

    override fun type(): StegType {
        return StegType.INNHENT_REGISTERDATA
    }
}
