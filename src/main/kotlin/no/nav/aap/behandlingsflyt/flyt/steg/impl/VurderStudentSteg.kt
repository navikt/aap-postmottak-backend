package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat

class VurderStudentSteg(
    private val behandlingTjeneste: BehandlingTjeneste,
    private val studentTjeneste: StudentRepository
) : BehandlingSteg {

    override fun utfør(input: StegInput): StegResultat {
        val behandling = behandlingTjeneste.hent(input.kontekst.behandlingId)
        val studentGrunnlag = studentTjeneste.hentHvisEksisterer(behandlingId = behandling.id)

        if (studentGrunnlag?.erKonsistent() != true) {
            return StegResultat(listOf(Definisjon.AVKLAR_STUDENT))
        }

        return StegResultat()
    }
}
